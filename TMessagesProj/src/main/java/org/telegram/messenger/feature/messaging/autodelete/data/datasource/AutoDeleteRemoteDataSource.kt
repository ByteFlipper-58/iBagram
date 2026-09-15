package org.telegram.messenger.feature.messaging.autodelete.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

class AutoDeleteRemoteDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    private val connectionsManager: ConnectionsManager?
        get() = if (isLegacyAvailable) ConnectionsManager.getInstance(currentAccount) else null

    suspend fun getDefaultHistoryTTL(): Result<Int> {
        val cm = connectionsManager ?: return Result.Success(0)

        return suspendCancellableCoroutine { continuation ->
            val req = TLRPC.TL_messages_getDefaultHistoryTTL()
            val reqId = cm.sendRequest(req) { response, error ->
                AndroidUtilities.runOnUIThread {
                    if (error != null) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error.text ?: "Failed to get default history TTL"))
                        }
                    } else if (response is TLRPC.TL_defaultHistoryTTL) {
                        if (continuation.isActive) {
                            continuation.resume(Result.Success(response.period))
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(Result.Success(0))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                try {
                    cm.cancelRequest(reqId, true)
                } catch (_: Throwable) {}
            }
        }
    }

    suspend fun setDefaultHistoryTTL(periodSeconds: Int): Result<Unit> {
        val cm = connectionsManager ?: return Result.Success(Unit)

        return suspendCancellableCoroutine { continuation ->
            val req = TLRPC.TL_messages_setDefaultHistoryTTL()
            req.period = periodSeconds
            val reqId = cm.sendRequest(req) { _, error ->
                AndroidUtilities.runOnUIThread {
                    if (error != null) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error.text ?: "Failed to set default history TTL"))
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resume(Result.Success(Unit))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                try {
                    cm.cancelRequest(reqId, true)
                } catch (_: Throwable) {}
            }
        }
    }
}
