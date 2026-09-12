package org.telegram.messenger.feature.network.proxy.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import kotlin.coroutines.resume

/**
 * Remote data source for proxy network checks and connection manager proxy settings.
 */
open class ProxyRemoteDataSource(currentAccount: Int) : BaseRemoteDataSource(currentAccount) {

    open suspend fun checkProxyPing(
        address: String,
        port: Int,
        username: String = "",
        password: String = "",
        secret: String = ""
    ): Result<Long> = suspendCancellableCoroutine { continuation ->
        try {
            val cm = ConnectionsManager.getInstance(currentAccount)
            val reqId = cm.checkProxy(address, port, username, password, secret) { time ->
                if (continuation.isActive) {
                    if (time == -1L) {
                        continuation.resume(Result.Failure(AppError.Network("Proxy check timed out or unreachable")))
                    } else {
                        continuation.resume(Result.Success(time))
                    }
                }
            }

            continuation.invokeOnCancellation {
                try {
                    cm.cancelRequest(reqId.toInt(), true)
                } catch (_: Throwable) {
                }
            }
        } catch (e: Throwable) {
            if (continuation.isActive) {
                continuation.resume(Result.Failure(AppError.Generic("Failed to start proxy ping check", e)))
            }
        }
    }

    open fun applyProxySettings(
        enabled: Boolean,
        address: String = "",
        port: Int = 0,
        username: String = "",
        password: String = "",
        secret: String = ""
    ) {
        try {
            ConnectionsManager.setProxySettings(enabled, address, port, username, password, secret)
        } catch (_: Throwable) {
        }
    }
}
