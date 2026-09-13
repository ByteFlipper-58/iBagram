package org.telegram.messenger.feature.messaging.ephemeralmessages.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_ephemeral
import kotlin.coroutines.resume

/**
 * Remote data source for Ephemeral Messages and bot command MTProto requests.
 */
open class EphemeralMessagesRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun sendEphemeralMessage(
        request: TL_ephemeral.TL_sendMessage
    ): Result<TLRPC.Updates> = suspendCancellableCoroutine { continuation ->
        val reqId = connectionsManager.sendRequest(request) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "sendEphemeralMessage failed")))
                    } else if (res is TLRPC.Updates) {
                        continuation.resume(Result.Success(res))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("Unexpected response type")))
                    }
                }
            }
        }

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(reqId, true)
        }
    }

    open suspend fun getChatFull(chatId: Long): Result<TLRPC.ChatFull?> = suspendCancellableCoroutine { continuation ->
        try {
            val controller = MessagesController.getInstance(currentAccount)
            val cached = controller.getChatFull(chatId)
            if (cached != null) {
                continuation.resume(Result.Success(cached))
                return@suspendCancellableCoroutine
            }
            controller.loadFullChat(chatId, 0, true)
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    continuation.resume(Result.Success(controller.getChatFull(chatId)))
                }
            }
        } catch (t: Throwable) {
            if (continuation.isActive) {
                continuation.resume(Result.Failure(AppError.Generic(t.message ?: "Failed to load chat full", t)))
            }
        }
    }
}
