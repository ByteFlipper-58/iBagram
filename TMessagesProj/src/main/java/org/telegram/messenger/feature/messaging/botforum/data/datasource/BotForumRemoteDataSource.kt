package org.telegram.messenger.feature.messaging.botforum.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_forum
import kotlin.coroutines.resume

/**
 * Remote data source for Bot Forum topic operations and streaming draft cancellations.
 */
open class BotForumRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun sendStopDraft(
        userId: Long,
        topicId: Long,
        randomId: Long
    ): Result<Boolean> = suspendCancellableCoroutine { continuation ->
        val action = TLRPC.TL_sendMessageStopDraftAction().apply {
            this.random_id = randomId
        }
        val req = TLRPC.TL_messages_setTyping().apply {
            this.peer = MessagesController.getInstance(currentAccount).getInputPeer(userId)
            this.action = action
            if (topicId != 0L) {
                this.flags = this.flags or TLObject.FLAG_0
                this.top_msg_id = topicId.toInt()
            }
        }

        val reqId = connectionsManager.sendRequest(req) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "sendStopDraft failed")))
                    } else {
                        continuation.resume(Result.Success(true))
                    }
                }
            }
        }

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(reqId, true)
        }
    }

    open suspend fun createForumTopic(
        peer: TLRPC.InputPeer,
        title: String,
        randomId: Long
    ): Result<TLRPC.Updates> = suspendCancellableCoroutine { continuation ->
        val req = TL_forum.TL_messages_createForumTopic().apply {
            this.title = if (title.isEmpty()) "#New Chat" else title
            this.title_missing = true
            this.peer = peer
            this.random_id = randomId
        }

        val reqId = connectionsManager.sendRequest(req) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "createForumTopic failed")))
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
}
