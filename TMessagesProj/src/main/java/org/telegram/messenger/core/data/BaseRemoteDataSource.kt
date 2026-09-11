package org.telegram.messenger.core.data

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import kotlin.coroutines.resume

/**
 * Base class for executing asynchronous network requests via MTProto ConnectionsManager
 * using coroutines with cancellation support for in-flight RPC requests.
 */
abstract class BaseRemoteDataSource(
    protected val currentAccount: Int
) {
    protected val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    /**
     * Executes a TLObject request asynchronously within a coroutine.
     * When the coroutine is cancelled, the RPC request is automatically cancelled in ConnectionsManager.
     */
    suspend fun <T : TLObject> executeRequest(
        request: TLObject,
        flags: Int = 0
    ): Result<T> = suspendCancellableCoroutine { continuation ->
        val requestId = connectionsManager.sendRequest(request, { response, error ->
            if (error != null) {
                continuation.resume(Result.failure(AppError.Network(error.text ?: "Unknown MTProto error", error.code)))
            } else if (response != null) {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(Result.success(response as T))
            } else {
                continuation.resume(Result.failure(AppError.Generic("Empty response received from MTProto")))
            }
        }, flags)

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(requestId, true)
        }
    }
}
