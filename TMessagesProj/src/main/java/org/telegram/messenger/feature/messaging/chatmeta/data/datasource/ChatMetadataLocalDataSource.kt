package org.telegram.messenger.feature.messaging.chatmeta.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.tgnet.ConnectionsManager
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_update

/**
 * Local data source managing metadata check intervals, active request throttling queues,
 * input peer resolution, and stats accumulation.
 */
open class ChatMetadataLocalDataSource(
    private val currentAccount: Int
) {
    private val lock = Any()
    private val reactionsRequests = mutableListOf<Int>()
    private val extendedMediaRequests = mutableListOf<Int>()

    @Volatile
    private var stats = ChatMetadataStatsModel()

    open fun getStats(): ChatMetadataStatsModel = synchronized(lock) { stats }

    open fun updateStats(newStats: ChatMetadataStatsModel) = synchronized(lock) {
        stats = newStats
    }

    open fun inspectItemsForUpdates(
        dialogId: Long,
        items: List<MessageMetadataCheckItem>,
        currentTime: Long
    ): ChatMetadataBatchResult = synchronized(lock) {
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

        val updated = stats.copy(
            activeDialogId = dialogId,
            totalReactionsCheckedCount = stats.totalReactionsCheckedCount + reactionsToCheck.size,
            totalExtendedMediaCheckedCount = stats.totalExtendedMediaCheckedCount + extendedMediaToCheck.size,
            totalStoriesCheckedCount = stats.totalStoriesCheckedCount + storiesToCheck.size
        )
        stats = updated

        ChatMetadataBatchResult(
            reactionMessageIds = reactionsToCheck,
            extendedMediaMessageIds = extendedMediaToCheck,
            storyIds = storiesToCheck
        )
    }

    open fun getInputPeer(dialogId: Long): TLRPC.InputPeer? {
        return runCatching {
            MessagesController.getInstance(currentAccount)?.getInputPeer(dialogId)
        }.getOrNull()
    }

    open fun trackReactionsRequest(reqId: Int) = synchronized(lock) {
        reactionsRequests.add(reqId)
        stats = stats.copy(activeReactionsRequestsCount = reactionsRequests.size)
        if (reactionsRequests.size > 5) {
            val oldest = reactionsRequests.removeAt(0)
            cancelRawRequest(oldest, true)
            stats = stats.copy(activeReactionsRequestsCount = reactionsRequests.size)
        }
    }

    open fun completeReactionsRequest(reqId: Int) = synchronized(lock) {
        reactionsRequests.remove(reqId)
        stats = stats.copy(activeReactionsRequestsCount = reactionsRequests.size)
    }

    open fun trackExtendedMediaRequest(reqId: Int) = synchronized(lock) {
        extendedMediaRequests.add(reqId)
        stats = stats.copy(activeExtendedMediaRequestsCount = extendedMediaRequests.size)
        if (extendedMediaRequests.size > 10) {
            val oldest = extendedMediaRequests.removeAt(0)
            cancelRawRequest(oldest, false)
            stats = stats.copy(activeExtendedMediaRequestsCount = extendedMediaRequests.size)
        }
    }

    open fun completeExtendedMediaRequest(reqId: Int) = synchronized(lock) {
        extendedMediaRequests.remove(reqId)
        stats = stats.copy(activeExtendedMediaRequestsCount = extendedMediaRequests.size)
    }

    open fun cancelAllPendingRequests() = synchronized(lock) {
        for (id in reactionsRequests) {
            cancelRawRequest(id, true)
        }
        reactionsRequests.clear()

        for (id in extendedMediaRequests) {
            cancelRawRequest(id, false)
        }
        extendedMediaRequests.clear()

        stats = stats.copy(
            activeReactionsRequestsCount = 0,
            activeExtendedMediaRequestsCount = 0
        )
    }

    open fun processReactionsUpdates(updates: TLRPC.Updates) {
        runCatching {
            for (update in updates.updates) {
                if (update is TL_update.TL_updateMessageReactions) {
                    update.updateUnreadState = false
                }
            }
            MessagesController.getInstance(currentAccount)?.processUpdates(updates, false)
        }
    }

    open fun processExtendedMediaUpdates(updates: TLRPC.Updates) {
        runCatching {
            MessagesController.getInstance(currentAccount)?.processUpdates(updates, false)
        }
    }

    private fun cancelRawRequest(reqId: Int, notifyServer: Boolean) {
        runCatching {
            ConnectionsManager.getInstance(currentAccount)?.cancelRequest(reqId, notifyServer)
        }
    }
}
