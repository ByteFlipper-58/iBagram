package org.telegram.messenger.feature.messaging.chatmeta.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_update

class LegacyChatMessagesMetadataRepository(
    private val account: Int
) : ChatMessagesMetadataRepository {

    private val lock = Any()

    private val _stats = MutableStateFlow(ChatMetadataStatsModel())

    private val reactionsRequests = mutableListOf<Int>()
    private val extendedMediaRequests = mutableListOf<Int>()

    override fun observeStats(): Flow<ChatMetadataStatsModel> = _stats.asStateFlow()

    override fun getStats(): ChatMetadataStatsModel = _stats.value

    override fun checkMessages(
        dialogId: Long,
        items: List<MessageMetadataCheckItem>,
        currentTime: Long
    ): Result<ChatMetadataBatchResult> {
        synchronized(lock) {
            val reactionsToCheck = mutableListOf<Int>()
            val extendedMediaToCheck = mutableListOf<Int>()
            val storiesToCheck = mutableListOf<Int>()

            for (item in items) {
                if (item.needsReactionsCheck(currentTime)) {
                    reactionsToCheck.add(item.messageId)
                }
                if (item.needsExtendedMediaCheck(currentTime)) {
                    extendedMediaToCheck.add(item.messageId)
                }
                if (item.needsStoryCheck(currentTime)) {
                    storiesToCheck.add(item.storyId)
                }
            }

            val currentStats = _stats.value
            _stats.value = currentStats.copy(
                activeDialogId = dialogId,
                totalReactionsCheckedCount = currentStats.totalReactionsCheckedCount + reactionsToCheck.size,
                totalExtendedMediaCheckedCount = currentStats.totalExtendedMediaCheckedCount + extendedMediaToCheck.size,
                totalStoriesCheckedCount = currentStats.totalStoriesCheckedCount + storiesToCheck.size
            )

            if (reactionsToCheck.isNotEmpty()) {
                loadReactions(dialogId, reactionsToCheck)
            }
            if (extendedMediaToCheck.isNotEmpty()) {
                loadExtendedMedia(dialogId, extendedMediaToCheck)
            }

            return Result.Success(
                ChatMetadataBatchResult(
                    reactionMessageIds = reactionsToCheck,
                    extendedMediaMessageIds = extendedMediaToCheck,
                    storyIds = storiesToCheck
                )
            )
        }
    }

    override fun loadReactions(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.Success(Unit)

        synchronized(lock) {
            if (ApplicationLoader.applicationContext == null) {
                return Result.Success(Unit)
            }

            try {
                val connectionsManager = ConnectionsManager.getInstance(account) ?: return Result.Success(Unit)
                val messagesController = org.telegram.messenger.MessagesController.getInstance(account) ?: return Result.Success(Unit)

                val req = TLRPC.TL_messages_getMessagesReactions()
                req.peer = messagesController.getInputPeer(dialogId)
                for (id in messageIds) {
                    req.id.add(id)
                }

                val reqIdRef = IntArray(1)
                reqIdRef[0] = connectionsManager.sendRequest(req) { response, error ->
                    if (error == null && response is TLRPC.Updates) {
                        for (update in response.updates) {
                            if (update is TL_update.TL_updateMessageReactions) {
                                update.updateUnreadState = false
                            }
                        }
                        messagesController.processUpdates(response, false)
                    }
                    synchronized(lock) {
                        reactionsRequests.remove(reqIdRef[0])
                        updateActiveRequestsCount()
                    }
                }

                reactionsRequests.add(reqIdRef[0])
                if (reactionsRequests.size > 5) {
                    val cancelId = reactionsRequests.removeAt(0)
                    connectionsManager.cancelRequest(cancelId, true)
                }
                updateActiveRequestsCount()
            } catch (e: Exception) {
                // Ignore gracefully
            }
            return Result.Success(Unit)
        }
    }

    override fun loadExtendedMedia(dialogId: Long, messageIds: List<Int>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.Success(Unit)

        synchronized(lock) {
            if (ApplicationLoader.applicationContext == null) {
                return Result.Success(Unit)
            }

            try {
                val connectionsManager = ConnectionsManager.getInstance(account) ?: return Result.Success(Unit)
                val messagesController = org.telegram.messenger.MessagesController.getInstance(account) ?: return Result.Success(Unit)

                val req = TLRPC.TL_messages_getExtendedMedia()
                req.peer = messagesController.getInputPeer(dialogId)
                for (id in messageIds) {
                    req.id.add(id)
                }

                val reqIdRef = IntArray(1)
                reqIdRef[0] = connectionsManager.sendRequest(req) { response, error ->
                    if (error == null && response is TLRPC.Updates) {
                        messagesController.processUpdates(response, false)
                    }
                    synchronized(lock) {
                        extendedMediaRequests.remove(reqIdRef[0])
                        updateActiveRequestsCount()
                    }
                }

                extendedMediaRequests.add(reqIdRef[0])
                if (extendedMediaRequests.size > 10) {
                    val cancelId = extendedMediaRequests.removeAt(0)
                    connectionsManager.cancelRequest(cancelId, false)
                }
                updateActiveRequestsCount()
            } catch (e: Exception) {
                // Ignore gracefully
            }
            return Result.Success(Unit)
        }
    }

    override fun cancelPendingRequests(): Result<Unit> {
        synchronized(lock) {
            if (ApplicationLoader.applicationContext != null) {
                try {
                    val connectionsManager = ConnectionsManager.getInstance(account)
                    if (connectionsManager != null) {
                        for (reqId in reactionsRequests) {
                            connectionsManager.cancelRequest(reqId, false)
                        }
                        for (reqId in extendedMediaRequests) {
                            connectionsManager.cancelRequest(reqId, false)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            reactionsRequests.clear()
            extendedMediaRequests.clear()
            updateActiveRequestsCount()
            return Result.Success(Unit)
        }
    }

    private fun updateActiveRequestsCount() {
        _stats.value = _stats.value.copy(
            activeReactionsRequestsCount = reactionsRequests.size,
            activeExtendedMediaRequestsCount = extendedMediaRequests.size
        )
    }
}
