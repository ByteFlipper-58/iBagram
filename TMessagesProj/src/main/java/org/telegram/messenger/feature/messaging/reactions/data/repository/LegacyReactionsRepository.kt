package org.telegram.messenger.feature.messaging.reactions.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.data.mapper.ReactionMapper
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionsSettingsModel
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import kotlin.coroutines.resume

class LegacyReactionsRepository(
    private val currentAccount: Int
) : ReactionsRepository {

    private val mediaDataController: MediaDataController
        get() = MediaDataController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private val connectionsManager: ConnectionsManager
        get() = ConnectionsManager.getInstance(currentAccount)

    override fun observeAvailableReactions(): Flow<List<ReactionItemModel>> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.reactionsDidLoad) {
                val list = mediaDataController.reactionsList?.map {
                    ReactionMapper.mapAvailableReaction(it)
                } ?: emptyList()
                trySend(list)
            }
        }

        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.reactionsDidLoad)

        val initial = mediaDataController.reactionsList?.map {
            ReactionMapper.mapAvailableReaction(it)
        } ?: emptyList()
        trySend(initial)

        awaitClose {
            NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.reactionsDidLoad)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getAvailableReactions(): Result<List<ReactionItemModel>> = withContext(Dispatchers.Main) {
        try {
            val list = mediaDataController.reactionsList?.map {
                ReactionMapper.mapAvailableReaction(it)
            } ?: emptyList()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get available reactions", e))
        }
    }

    override suspend fun loadAvailableReactions(force: Boolean): Result<List<ReactionItemModel>> = withContext(Dispatchers.Main) {
        try {
            mediaDataController.loadReactions(!force, null)
            val list = mediaDataController.reactionsList?.map {
                ReactionMapper.mapAvailableReaction(it)
            } ?: emptyList()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to load available reactions", e))
        }
    }

    override fun observeRecentReactions(): Flow<List<ReactionItemModel>> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.reactionsDidLoad) {
                val recent = mediaDataController.recentReactions?.map {
                    ReactionMapper.mapReaction(it)
                } ?: emptyList()
                trySend(recent)
            }
        }

        NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.reactionsDidLoad)

        val initial = mediaDataController.recentReactions?.map {
            ReactionMapper.mapReaction(it)
        } ?: emptyList()
        trySend(initial)

        awaitClose {
            NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.reactionsDidLoad)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getRecentReactions(): Result<List<ReactionItemModel>> = withContext(Dispatchers.Main) {
        try {
            val recent = mediaDataController.recentReactions?.map {
                ReactionMapper.mapReaction(it)
            } ?: emptyList()
            Result.Success(recent)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get recent reactions", e))
        }
    }

    override suspend fun getReactionsSettings(): Result<ReactionsSettingsModel> = withContext(Dispatchers.Main) {
        try {
            val doubleTap = mediaDataController.doubleTapReaction
            val available = mediaDataController.reactionsList?.map {
                ReactionMapper.mapAvailableReaction(it)
            } ?: emptyList()
            val recent = mediaDataController.recentReactions?.map {
                ReactionMapper.mapReaction(it)
            } ?: emptyList()
            val top = mediaDataController.topReactions?.map {
                ReactionMapper.mapReaction(it)
            } ?: emptyList()

            Result.Success(
                ReactionsSettingsModel(
                    doubleTapReaction = doubleTap,
                    availableReactions = available,
                    recentReactions = recent,
                    topReactions = top
                )
            )
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get reactions settings", e))
        }
    }

    override suspend fun getDoubleTapReaction(): Result<String?> = withContext(Dispatchers.Main) {
        try {
            Result.Success(mediaDataController.doubleTapReaction)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get double tap reaction", e))
        }
    }

    override suspend fun setDoubleTapReaction(reaction: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            mediaDataController.doubleTapReaction = reaction
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to set double tap reaction", e))
        }
    }

    override suspend fun sendReaction(
        dialogId: Long,
        messageId: Int,
        reactions: List<ReactionItemModel>,
        isBig: Boolean,
        addToRecent: Boolean
    ): Result<Unit> = withContext(Dispatchers.Main) {
        val inputPeer = messagesController.getInputPeer(dialogId)
            ?: return@withContext Result.Failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

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
            val requestId = connectionsManager.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(
                        Result.Failure(AppError.Network("Failed to send reaction: ${error.text} (${error.code})"))
                    )
                } else {
                    if (response is TLRPC.Updates) {
                        messagesController.processUpdates(response, false)
                    }
                    continuation.resume(Result.Success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, true)
            }
        }
    }

    override suspend fun clearReactions(dialogId: Long, messageId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        val inputPeer = messagesController.getInputPeer(dialogId)
            ?: return@withContext Result.Failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

        val req = TLRPC.TL_messages_sendReaction().apply {
            peer = inputPeer
            msg_id = messageId
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = connectionsManager.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(
                        Result.Failure(AppError.Network("Failed to clear reactions: ${error.text} (${error.code})"))
                    )
                } else {
                    if (response is TLRPC.Updates) {
                        messagesController.processUpdates(response, false)
                    }
                    continuation.resume(Result.Success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, true)
            }
        }
    }

    override suspend fun sendVote(
        dialogId: Long,
        messageId: Int,
        pollId: Long,
        options: List<ByteArray>
    ): Result<Unit> = withContext(Dispatchers.Main) {
        val inputPeer = messagesController.getInputPeer(dialogId)
            ?: return@withContext Result.Failure(AppError.NotFound("Input peer not found for dialogId: $dialogId"))

        val req = TLRPC.TL_messages_sendVote().apply {
            peer = inputPeer
            msg_id = messageId
            for (opt in options) {
                this.options.add(opt)
            }
        }

        suspendCancellableCoroutine<Result<Unit>> { continuation ->
            val requestId = connectionsManager.sendRequest(req, { response, error ->
                if (error != null) {
                    continuation.resume(
                        Result.Failure(AppError.Network("Failed to send vote: ${error.text} (${error.code})"))
                    )
                } else {
                    if (response is TLRPC.Updates) {
                        messagesController.processUpdates(response, false)
                    }
                    continuation.resume(Result.Success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                connectionsManager.cancelRequest(requestId, true)
            }
        }
    }
}
