package org.telegram.messenger.feature.messaging.topics.data.datasource

import androidx.collection.LongSparseArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.TopicsController
import org.telegram.tgnet.TLRPC

/**
 * Local data source managing forum topics caches, SQLite database interactions,
 * and legacy TopicsController state.
 */
open class TopicsLocalDataSource(
    protected val currentAccount: Int,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // In-memory fallback cache for headless environments and testing
    private val memoryTopics = LongSparseArray<ArrayList<TLRPC.TL_forumTopic>>()

    protected open fun getTopicsController(): TopicsController? {
        return try {
            MessagesController.getInstance(currentAccount)?.topicsController
        } catch (_: Throwable) {
            null
        }
    }

    protected open fun getMessagesStorage(): MessagesStorage? {
        return try {
            MessagesStorage.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getTopics(chatId: Long): List<TLRPC.TL_forumTopic> {
        return try {
            val controller = getTopicsController()
            if (controller != null) {
                controller.getTopics(chatId) ?: emptyList()
            } else {
                memoryTopics.get(chatId) ?: emptyList()
            }
        } catch (_: Throwable) {
            memoryTopics.get(chatId) ?: emptyList()
        }
    }

    open fun findTopic(chatId: Long, topicId: Long): TLRPC.TL_forumTopic? {
        return try {
            val controller = getTopicsController()
            if (controller != null) {
                controller.findTopic(chatId, topicId)
            } else {
                memoryTopics.get(chatId)?.find { it.id.toLong() == topicId }
            }
        } catch (_: Throwable) {
            memoryTopics.get(chatId)?.find { it.id.toLong() == topicId }
        }
    }

    open fun putTopicInMemory(chatId: Long, topic: TLRPC.TL_forumTopic) {
        var list = memoryTopics.get(chatId)
        if (list == null) {
            list = ArrayList()
            memoryTopics.put(chatId, list)
        }
        val existingIndex = list.indexOfFirst { it.id == topic.id }
        if (existingIndex >= 0) {
            list[existingIndex] = topic
        } else {
            list.add(topic)
        }
    }

    open fun loadTopics(chatId: Long, fromCache: Boolean, loadType: Int) {
        try {
            getTopicsController()?.loadTopics(chatId, fromCache, loadType)
        } catch (_: Throwable) {
        }
    }

    open fun reloadTopics(chatId: Long) {
        try {
            getTopicsController()?.reloadTopics(chatId)
        } catch (_: Throwable) {
        }
    }

    open fun toggleCloseTopic(chatId: Long, topicId: Int, close: Boolean) {
        try {
            getTopicsController()?.toggleCloseTopic(chatId, topicId, close)
        } catch (_: Throwable) {
        }
        findTopic(chatId, topicId.toLong())?.let {
            it.closed = close
        }
    }

    open fun pinTopic(chatId: Long, topicId: Int, pin: Boolean) {
        try {
            getTopicsController()?.pinTopic(chatId, topicId, pin, null)
        } catch (_: Throwable) {
        }
        findTopic(chatId, topicId.toLong())?.let {
            it.pinned = pin
        }
    }

    open fun toggleShowTopic(chatId: Long, topicId: Int, show: Boolean) {
        try {
            getTopicsController()?.toggleShowTopic(chatId, topicId, show)
        } catch (_: Throwable) {
        }
        findTopic(chatId, topicId.toLong())?.let {
            it.hidden = !show
        }
    }

    open fun deleteTopics(chatId: Long, topicIds: List<Long>) {
        try {
            val ids = ArrayList<Int>(topicIds.size)
            for (id in topicIds) {
                ids.add(id.toInt())
            }
            getTopicsController()?.deleteTopics(chatId, ids)
        } catch (_: Throwable) {
        }
        memoryTopics.get(chatId)?.removeAll { topicIds.contains(it.id.toLong()) }
    }

    open fun reorderPinnedTopics(chatId: Long, topicIds: List<Long>) {
        try {
            val ids = ArrayList<Int>(topicIds.size)
            for (id in topicIds) {
                ids.add(id.toInt())
            }
            getTopicsController()?.reorderPinnedTopics(chatId, ids)
        } catch (_: Throwable) {
        }
    }

    open fun markAllReactionsAsRead(chatId: Long, topicId: Long) {
        try {
            getTopicsController()?.markAllReactionsAsRead(chatId, topicId)
        } catch (_: Throwable) {
        }
        findTopic(chatId, topicId)?.let {
            it.unread_reactions_count = 0
        }
    }

    open fun getForumUnreadCount(chatId: Long): IntArray? {
        return try {
            getTopicsController()?.getForumUnreadCount(chatId)
        } catch (_: Throwable) {
            null
        }
    }

    open suspend fun loadTopicsFromDatabase(
        chatId: Long,
        callback: (ArrayList<TLRPC.TL_forumTopic>?) -> Unit
    ) = withContext(ioDispatcher) {
        try {
            getMessagesStorage()?.loadTopics(-chatId, callback) ?: callback(null)
        } catch (_: Throwable) {
            callback(null)
        }
    }

    open suspend fun saveTopicToDatabase(
        chatId: Long,
        topic: TLRPC.TL_forumTopic
    ) = withContext(ioDispatcher) {
        try {
            getMessagesStorage()?.saveTopics(-chatId, java.util.Collections.singletonList(topic), false, true, 0)
        } catch (_: Throwable) {
        }
    }

    open suspend fun removeTopicFromDatabase(
        chatId: Long,
        topicId: Long
    ) = withContext(ioDispatcher) {
        try {
            getMessagesStorage()?.removeTopic(-chatId, topicId)
        } catch (_: Throwable) {
        }
    }
}
