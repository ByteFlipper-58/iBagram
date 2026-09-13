package org.telegram.messenger.feature.security.sessions.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.data.datasource.SessionsLocalDataSource
import org.telegram.messenger.feature.security.sessions.data.datasource.SessionsRemoteDataSource
import org.telegram.messenger.feature.security.sessions.data.mapper.SessionMapper
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository

/**
 * Clean repository implementation coordinating [SessionsLocalDataSource] and [SessionsRemoteDataSource].
 */
open class SessionsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: SessionsLocalDataSource,
    private val remoteDataSource: SessionsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionsRepository {

    override fun observeSessions(): Flow<SessionsListModel> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.newSessionReceived)
            .map {
                loadSessions().getOrDefault(localDataSource.getCachedSessions())
            }
            .onStart {
                emit(getSessions().getOrDefault(localDataSource.getCachedSessions()))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun observeWebSessions(): Flow<List<WebSessionModel>> {
        return NotificationCenterFlowBridge.observeEvent(currentAccount, NotificationCenter.newSessionReceived)
            .map {
                loadWebSessions().getOrDefault(localDataSource.getCachedWebSessions())
            }
            .onStart {
                emit(getWebSessions().getOrDefault(localDataSource.getCachedWebSessions()))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getSessions(): Result<SessionsListModel> = withContext(mainDispatcher) {
        val cached = localDataSource.getCachedSessions()
        if (cached.currentSession != null || cached.otherSessions.isNotEmpty()) {
            Result.Success(cached)
        } else {
            loadSessions()
        }
    }

    override suspend fun loadSessions(): Result<SessionsListModel> = withContext(mainDispatcher) {
        val result = remoteDataSource.getAuthorizations()
        when (result) {
            is Result.Success -> {
                val mapped = SessionMapper.mapAuthorizations(result.data)
                localDataSource.putCachedSessions(mapped)
                Result.Success(mapped)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun getWebSessions(): Result<List<WebSessionModel>> = withContext(mainDispatcher) {
        val cached = localDataSource.getCachedWebSessions()
        if (cached.isNotEmpty()) {
            Result.Success(cached)
        } else {
            loadWebSessions()
        }
    }

    override suspend fun loadWebSessions(): Result<List<WebSessionModel>> = withContext(mainDispatcher) {
        val result = remoteDataSource.getWebAuthorizations()
        when (result) {
            is Result.Success -> {
                runCatching {
                    MessagesController.getInstance(currentAccount)?.putUsers(result.data.users, false)
                }
                val mapped = result.data.authorizations.map { SessionMapper.mapWebSession(it) }
                localDataSource.putCachedWebSessions(mapped)
                Result.Success(mapped)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateSession(hash: Long): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.resetAuthorization(hash)
        when (result) {
            is Result.Success -> {
                localDataSource.removeSession(hash)
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateAllOtherSessions(): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.resetAllAuthorizations()
        when (result) {
            is Result.Success -> {
                localDataSource.clearOtherSessions()
                localDataSource.refreshPushTokensAfterResetAll()
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateWebSession(hash: Long): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.resetWebAuthorization(hash)
        when (result) {
            is Result.Success -> {
                localDataSource.removeWebSession(hash)
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun terminateAllWebSessions(): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.resetAllWebAuthorizations()
        when (result) {
            is Result.Success -> {
                localDataSource.clearAllWebSessions()
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
        val result = remoteDataSource.changeAuthorizationSettings(
            hash = hash,
            encryptedRequestsDisabled = !acceptSecretChats,
            callRequestsDisabled = !acceptCalls
        )
        when (result) {
            is Result.Success -> {
                localDataSource.updateSessionSettings(hash, acceptSecretChats, acceptCalls)
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun setSessionsTtl(ttlDays: Int): Result<Unit> = withContext(mainDispatcher) {
        val result = remoteDataSource.setAuthorizationTTL(ttlDays)
        when (result) {
            is Result.Success -> {
                localDataSource.setSessionsTtl(ttlDays)
                Result.Success(Unit)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun acceptQrLogin(token: ByteArray): Result<Unit> = withContext(mainDispatcher) {
        if (token.isEmpty()) {
            return@withContext Result.Failure(AppError.InvalidInput("QR login token is empty"))
        }
        val result = remoteDataSource.acceptLoginToken(token)
        when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> result
        }
    }

    override suspend fun acceptQrLoginByLink(link: String): Result<Unit> = withContext(mainDispatcher) {
        try {
            val token = localDataSource.decodeTokenFromLink(link)
            acceptQrLogin(token)
        } catch (e: Throwable) {
            Result.Failure(AppError.InvalidInput("Invalid QR code login link: ${e.message}"))
        }
    }
}
