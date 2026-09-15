package org.telegram.messenger.feature.messaging.reactions.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.data.mapper.ReactionMapper
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

/**
 * Удаленный источник данных для взаимодействия с RPC-методами реакций Telegram.
 */
class ReactionsRemoteDataSource(
    private val currentAccount: Int
) {

    private val mediaDataController: MediaDataController?
        get() = try {
            MediaDataController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    private val connectionsManager: ConnectionsManager?
        get() = try {
            ConnectionsManager.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }

    suspend fun loadAvailableReactions(force: Boolean = false): Result<List<ReactionItemModel>> = withContext(Dispatchers.Main) {
        try {
            val controller = mediaDataController ?: return@withContext Result.failure(AppError.NotFound("MediaDataController not available"))
            controller.loadReactions(!force, null)
            val list = controller.reactionsList?.map { ReactionMapper.mapAvailableReaction(it) } ?: emptyList()
            Result.success(list)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to load reactions", e))
        }
    }

    suspend fun sendReaction(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean,
        addToRecent: Boolean
    ): Result<Unit> = withContext(Dispatchers.Main) {
        val mc = messagesController ?: return@withContext Result.failure(AppError.NotFound("MessagesController not available"))
        val cm = connectionsManager ?: return@withContext Result.failure(AppError.Network("ConnectionsManager not available"))

        val inputPeer = mc.getInputPeer(dialogId)
            ?: return@withContext Result.failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

        val req = TLRPC.TL_messages_sendReaction().apply {
            peer = inputPeer
            msg_id = messageId
            add_to_recent = addToRecent
            if (isBig) {
                flags = flags or 2
                big = true
            }
            if (reactions.isNotEmpty()) {
                flags = flags or 1
                for (r in reactions) {
                    reaction.add(ReactionMapper.toTLReaction(r))
                }
            }
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(AppError.Network("Failed to send reaction: ${error.text} (${error.code})")))
                } else {
                    if (response is TLRPC.Updates) {
                        mc.processUpdates(response, false)
                    }
                    continuation.resume(Result.success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                cm.cancelRequest(requestId, true)
            }
        }
    }

    suspend fun clearReactions(dialogId: Long, messageId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val mc = messagesController ?: return@withContext Result.failure(AppError.NotFound("MessagesController not available"))
        val cm = connectionsManager ?: return@withContext Result.failure(AppError.Network("ConnectionsManager not available"))

        val inputPeer = mc.getInputPeer(dialogId)
            ?: return@withContext Result.failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

        val req = TLRPC.TL_messages_sendReaction().apply {
            peer = inputPeer
            msg_id = messageId
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(AppError.Network("Failed to clear reactions: ${error.text} (${error.code})")))
                } else {
                    if (response is TLRPC.Updates) {
                        mc.processUpdates(response, false)
                    }
                    continuation.resume(Result.success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                cm.cancelRequest(requestId, true)
            }
        }
    }

    suspend fun sendVote(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ): Result<Unit> = withContext(Dispatchers.Main) {
        val mc = messagesController ?: return@withContext Result.failure(AppError.NotFound("MessagesController not available"))
        val cm = connectionsManager ?: return@withContext Result.failure(AppError.Network("ConnectionsManager not available"))

        val inputPeer = mc.getInputPeer(dialogId)
            ?: return@withContext Result.failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

        val req = TLRPC.TL_messages_sendVote().apply {
            peer = inputPeer
            msg_id = messageId
            for (opt in options) {
                this.options.add(opt)
            }
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = cm.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(AppError.Network("Failed to send vote: ${error.text} (${error.code})")))
                } else {
                    if (response is TLRPC.Updates) {
                        mc.processUpdates(response, false)
                    }
                    continuation.resume(Result.success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                cm.cancelRequest(requestId, true)
            }
        }
    }
}
