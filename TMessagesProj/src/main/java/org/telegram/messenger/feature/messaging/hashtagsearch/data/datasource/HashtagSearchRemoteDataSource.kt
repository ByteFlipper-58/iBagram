package org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource

import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

/**
 * Remote data source for MTProto hashtag search queries and username resolution.
 */
open class HashtagSearchRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun searchGlobal(
        query: String,
        limit: Int = 21,
        offsetRate: Int = 0,
        offsetPeer: TLRPC.InputPeer? = null,
        offsetId: Int = 0
    ): Result<TLRPC.messages_Messages> = suspendCancellableCoroutine { continuation ->
        val req = TLRPC.TL_messages_searchGlobal().apply {
            this.limit = limit
            this.q = query
            this.filter = TLRPC.TL_inputMessagesFilterEmpty()
            this.offset_peer = offsetPeer ?: TLRPC.TL_inputPeerEmpty()
            this.offset_rate = offsetRate
            this.offset_id = offsetId
        }

        val reqId = connectionsManager.sendRequest(req) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "searchGlobal failed")))
                    } else if (res is TLRPC.messages_Messages) {
                        continuation.resume(Result.Success(res))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("searchGlobal unexpected response type")))
                    }
                }
            }
        }

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(reqId, true)
        }
    }

    open suspend fun searchChat(
        peer: TLRPC.InputPeer,
        hashtag: String,
        limit: Int = 21,
        offsetId: Int = 0
    ): Result<TLRPC.messages_Messages> = suspendCancellableCoroutine { continuation ->
        val req = TLRPC.TL_messages_search().apply {
            this.filter = TLRPC.TL_inputMessagesFilterEmpty()
            this.peer = peer
            this.q = hashtag
            this.limit = limit
            if (offsetId != 0) {
                this.offset_id = offsetId
            }
        }

        val reqId = connectionsManager.sendRequest(req) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "searchChat failed")))
                    } else if (res is TLRPC.messages_Messages) {
                        continuation.resume(Result.Success(res))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("searchChat unexpected response type")))
                    }
                }
            }
        }

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(reqId, true)
        }
    }

    open suspend fun searchPosts(
        query: String,
        limit: Int = 21,
        offsetRate: Int = 0,
        offsetPeer: TLRPC.InputPeer? = null,
        offsetId: Int = 0
    ): Result<TLRPC.messages_Messages> = suspendCancellableCoroutine { continuation ->
        val req = TLRPC.TL_channels_searchPosts().apply {
            this.flags = this.flags or 1
            this.hashtag = query
            this.limit = limit
            this.offset_peer = offsetPeer ?: TLRPC.TL_inputPeerEmpty()
            this.offset_rate = offsetRate
            this.offset_id = offsetId
        }

        val reqId = connectionsManager.sendRequest(req) { res, err ->
            AndroidUtilities.runOnUIThread {
                if (continuation.isActive) {
                    if (err != null) {
                        continuation.resume(Result.Failure(AppError.Network(err.text ?: "searchPosts failed")))
                    } else if (res is TLRPC.messages_Messages) {
                        continuation.resume(Result.Success(res))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("searchPosts unexpected response type")))
                    }
                }
            }
        }

        continuation.invokeOnCancellation {
            connectionsManager.cancelRequest(reqId, true)
        }
    }

    open suspend fun resolveUsername(username: String): Result<TLObject?> =
        suspendCancellableCoroutine { continuation ->
            try {
                val controller = MessagesController.getInstance(currentAccount)
                val existing = controller.getUserOrChat(username)
                if (existing != null) {
                    continuation.resume(Result.Success(existing))
                    return@suspendCancellableCoroutine
                }

                val cancel = controller.userNameResolver.resolve(username) {
                    AndroidUtilities.runOnUIThread {
                        if (continuation.isActive) {
                            val resolved = controller.getUserOrChat(username)
                            continuation.resume(Result.Success(resolved))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    cancel?.run()
                }
            } catch (t: Throwable) {
                if (continuation.isActive) {
                    continuation.resume(Result.Failure(AppError.Generic(t.message ?: "Failed to resolve username", t)))
                }
            }
        }
}
