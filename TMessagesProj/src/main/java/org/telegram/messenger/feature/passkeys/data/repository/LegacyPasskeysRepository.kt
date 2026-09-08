package org.telegram.messenger.feature.passkeys.data.repository

import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BuildVars
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.passkeys.data.mapper.PasskeyMapper
import org.telegram.messenger.feature.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.passkeys.domain.model.PasskeysStateModel
import org.telegram.messenger.feature.passkeys.domain.repository.PasskeysRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import kotlin.coroutines.resume

class LegacyPasskeysRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PasskeysRepository {

    private val _passkeysState = MutableStateFlow(PasskeysStateModel())

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    override fun observePasskeys(): Flow<PasskeysStateModel> = _passkeysState.asStateFlow()

    override suspend fun isSupported(): Boolean = withContext(mainDispatcher) {
        Build.VERSION.SDK_INT >= 28 && BuildVars.SUPPORTS_PASSKEYS
    }

    override suspend fun getMaxPasskeys(): Int = withContext(mainDispatcher) {
        try {
            messagesController.config.passkeysAccountPasskeysMax.get()
        } catch (_: Throwable) {
            5
        }
    }

    override suspend fun getPasskeys(force: Boolean): Result<List<PasskeyModel>> =
        withContext(mainDispatcher) {
            val supported = isSupported()
            val max = getMaxPasskeys()

            if (!supported) {
                val emptyState = PasskeyMapper.toState(emptyList(), max, false)
                _passkeysState.value = emptyState
                return@withContext Result.Success(emptyList())
            }

            if (!force && _passkeysState.value.passkeys.isNotEmpty()) {
                return@withContext Result.Success(_passkeysState.value.passkeys)
            }

            suspendCancellableCoroutine { continuation ->
                val req = TL_account.getPasskeys()
                val reqId = connectionsManager.sendRequestTyped(req, AndroidUtilities::runOnUIThread) { response, error ->
                    if (error != null) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error.text ?: "Failed to get passkeys"))
                        }
                    } else if (response is TL_account.Passkeys) {
                        val domainList = PasskeyMapper.toDomainList(response.passkeys)
                        val newState = PasskeyMapper.toState(domainList, max, true)
                        _passkeysState.value = newState
                        if (continuation.isActive) {
                            continuation.resume(Result.Success(domainList))
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure("Unexpected passkeys response"))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    connectionsManager.cancelRequest(reqId, true)
                }
            }
        }

    override suspend fun deletePasskey(id: String): Result<Unit> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val req = TL_account.deletePasskey().apply {
                this.id = id
            }
            val reqId = connectionsManager.sendRequestTyped(req, AndroidUtilities::runOnUIThread) { response, error ->
                if (error != null) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error.text ?: "Failed to delete passkey"))
                    }
                } else if (response is TLRPC.TL_boolFalse) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure("Could not delete passkey"))
                    }
                } else {
                    val updatedList = _passkeysState.value.passkeys.filter { it.id != id }
                    _passkeysState.value = _passkeysState.value.copy(passkeys = updatedList)
                    if (continuation.isActive) {
                        continuation.resume(Result.Success(Unit))
                    }
                }
            }

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
    }
}
