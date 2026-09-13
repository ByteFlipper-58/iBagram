package org.telegram.messenger.feature.messaging.aitones.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_aicompose
import kotlin.coroutines.resume

/**
 * Remote data source for MTProto AI Compose tone actions.
 */
open class AiTonesRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun getTones(hash: Long): Result<TL_aicompose.Tones> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_aicompose.getTones().apply {
                this.hash = hash
            }
            val reqId = connectionsManager.sendRequestTyped(
                req,
                AndroidUtilities::runOnUIThread
            ) { res, err ->
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "getTones failed")))
                    } else if (res != null) {
                        continuation.resume(Result.Success(res))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("getTones returned null response")))
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }

    open suspend fun unsaveTone(tone: TL_aicompose.AiComposeTone): Result<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val req = TL_aicompose.saveTone().apply {
                this.tone = TL_aicompose.InputAiComposeTone.from(tone)
                this.unsave = true
            }
            val reqId = connectionsManager.sendRequest(req) { res, err ->
                AndroidUtilities.runOnUIThread {
                    if (continuation.isActive) {
                        if (err != null) {
                            continuation.resume(Result.Failure(AppError.Network(err.text ?: "unsaveTone failed")))
                        } else {
                            continuation.resume(Result.Success(res is TLRPC.TL_boolTrue))
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(reqId, true)
            }
        }
}
