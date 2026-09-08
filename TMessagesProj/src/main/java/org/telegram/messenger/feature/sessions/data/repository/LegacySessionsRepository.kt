package org.telegram.messenger.feature.sessions.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.data.mapper.SessionMapper
import org.telegram.messenger.feature.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.sessions.domain.model.WebSessionModel
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.Base64
import kotlin.coroutines.resume

/**
 * Adapter implementing [SessionsRepository] on top of Telegram's [ConnectionsManager],
 * [MessagesController], and MTProto account/auth requests.
 * All state mutations and controller interactions run safely on [Dispatchers.Main].
 */
class LegacySessionsRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionsRepository {

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    @Volatile
    private var cachedSessions: SessionsListModel = SessionsListModel()

    @Volatile
    private var cachedWebSessions: List<WebSessionModel> = emptyList()

    override fun observeSessions(): Flow<SessionsListModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.newSessionReceived)
            .map {
                loadSessions().getOrDefault(cachedSessions)
            }
            .onStart {
                emit(getSessions().getOrDefault(cachedSessions))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun observeWebSessions(): Flow<List<WebSessionModel>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.newSessionReceived)
            .map {
                loadWebSessions().getOrDefault(cachedWebSessions)
            }
            .onStart {
                emit(getWebSessions().getOrDefault(cachedWebSessions))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getSessions(): Result<SessionsListModel> = withContext(mainDispatcher) {
        if (cachedSessions.currentSession != null || cachedSessions.otherSessions.isNotEmpty()) {
            Result.Success(cachedSessions)
        } else {
            loadSessions()
        }
    }

    override suspend fun loadSessions(): Result<SessionsListModel> = withContext(mainDispatcher) {
        val req = TL_account.getAuthorizations()
        val result: Result<TL_account.authorizations> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                val mapped = SessionMapper.mapAuthorizations(result.data)
                cachedSessions = mapped
                Result.Success(mapped)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun getWebSessions(): Result<List<WebSessionModel>> = withContext(mainDispatcher) {
        if (cachedWebSessions.isNotEmpty()) {
            Result.Success(cachedWebSessions)
        } else {
            loadWebSessions()
        }
    }

    override suspend fun loadWebSessions(): Result<List<WebSessionModel>> = withContext(mainDispatcher) {
        val req = TL_account.getWebAuthorizations()
        val result: Result<TL_account.webAuthorizations> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                messagesController.putUsers(result.data.users, false)
                val mapped = result.data.authorizations.map { SessionMapper.mapWebSession(it) }
                cachedWebSessions = mapped
                Result.Success(mapped)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateSession(hash: Long): Result<Unit> = withContext(mainDispatcher) {
        val req = TL_account.resetAuthorization().apply {
            this.hash = hash
        }
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                cachedSessions = cachedSessions.copy(
                    otherSessions = cachedSessions.otherSessions.filter { it.hash != hash },
                    passwordPendingSessions = cachedSessions.passwordPendingSessions.filter { it.hash != hash }
                )
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateAllOtherSessions(): Result<Unit> = withContext(mainDispatcher) {
        val req = TLRPC.TL_auth_resetAuthorizations()
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                cachedSessions = cachedSessions.copy(
                    otherSessions = emptyList(),
                    passwordPendingSessions = emptyList()
                )

                // Refresh push tokens across active user accounts (matching Telegram SessionsActivity)
                for (a in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                    val userConfig = UserConfig.getInstance(a)
                    if (!userConfig.isClientActivated) {
                        continue
                    }
                    userConfig.registeredForPush = false
                    userConfig.saveConfig(false)
                    MessagesController.getInstance(a).registerForPush(SharedConfig.pushType, SharedConfig.pushString)
                    ConnectionsManager.getInstance(a).setUserId(userConfig.clientUserId)
                }

                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateWebSession(hash: Long): Result<Unit> = withContext(mainDispatcher) {
        val req = TL_account.resetWebAuthorization().apply {
            this.hash = hash
        }
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                cachedWebSessions = cachedWebSessions.filter { it.hash != hash }
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateAllWebSessions(): Result<Unit> = withContext(mainDispatcher) {
        val req = TL_account.resetWebAuthorizations()
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                cachedWebSessions = emptyList()
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun updateSessionSettings(
        hash: Long,
        acceptSecretChats: Boolean,
        acceptCalls: Boolean
    ): Result<Unit> = withContext(mainDispatcher) {
        val req = TL_account.changeAuthorizationSettings().apply {
            this.hash = hash
            this.encrypted_requests_disabled = !acceptSecretChats
            this.call_requests_disabled = !acceptCalls
            this.flags = 1 or 2
        }
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                val updateItem: (org.telegram.messenger.feature.sessions.domain.model.SessionModel) -> org.telegram.messenger.feature.sessions.domain.model.SessionModel = { s ->
                    if (s.hash == hash) {
                        s.copy(
                            acceptSecretChats = acceptSecretChats,
                            acceptCalls = acceptCalls
                        )
                    } else s
                }
                cachedSessions = cachedSessions.copy(
                    currentSession = cachedSessions.currentSession?.let(updateItem),
                    otherSessions = cachedSessions.otherSessions.map(updateItem),
                    passwordPendingSessions = cachedSessions.passwordPendingSessions.map(updateItem)
                )
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun setSessionsTtl(ttlDays: Int): Result<Unit> = withContext(mainDispatcher) {
        val req = TL_account.setAuthorizationTTL().apply {
            this.authorization_ttl_days = ttlDays
        }
        val result: Result<TLRPC.TL_boolTrue> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                cachedSessions = cachedSessions.copy(ttlDays = ttlDays)
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun acceptQrLogin(token: ByteArray): Result<Unit> = withContext(mainDispatcher) {
        val req = TLRPC.TL_auth_acceptLoginToken().apply {
            this.token = token
        }
        val result: Result<TLRPC.TL_authorization> = sendTlRequest(req)
        when (result) {
            is Result.Success -> {
                loadSessions()
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun acceptQrLoginByLink(link: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val token = decodeTokenFromLink(link)
            acceptQrLogin(token)
        } catch (e: Throwable) {
            Result.Failure(AppError.InvalidInput("Invalid QR code login link"))
        }
    }

    private fun decodeTokenFromLink(link: String): ByteArray {
        var code = link.trim()
        val prefix = "tg://login?token="
        if (code.startsWith(prefix, ignoreCase = true)) {
            code = code.substring(prefix.length)
        } else if (code.contains("token=", ignoreCase = true)) {
            code = code.substringAfter("token=")
        }
        val clean = code.replace('/', '_').replace('+', '-')
        return try {
            Base64.getUrlDecoder().decode(clean)
        } catch (e: Throwable) {
            try {
                android.util.Base64.decode(clean, android.util.Base64.URL_SAFE)
            } catch (e2: Throwable) {
                Base64.getDecoder().decode(clean)
            }
        }
    }

    private suspend inline fun <reified T : TLObject> sendTlRequest(req: TLObject): Result<T> {
        return suspendCancellableCoroutine { continuation ->
            val reqId = connectionsManager.sendRequest(req) { response, error ->
                if (error != null) {
                    continuation.resume(Result.Failure(AppError.Network(error.text ?: "Network error", error.code)))
                } else if (response is T) {
                    continuation.resume(Result.Success(response))
                } else if (response == null) {
                    continuation.resume(Result.Failure(AppError.Network("Empty response from Telegram server", 0)))
                } else {
                    continuation.resume(Result.Failure(AppError.Generic("Unexpected response type: ${response.javaClass.simpleName}")))
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
    }
}
