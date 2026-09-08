package org.telegram.messenger.feature.topics.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.topics.domain.model.TopicModel

/**
 * Domain repository contract for managing forum topics.
 */
interface TopicsRepository {

    /**
     * Observes topics for the given supergroup chatId.
     */
    fun observeTopics(chatId: Long): Flow<List<TopicModel>>

    /**
     * Observes unread count for the given supergroup forum chatId.
     */
    fun observeForumUnreadCount(chatId: Long): Flow<ForumUnreadCountModel>

    /**
     * Returns the cached topics for the given chatId.
     */
    suspend fun getTopics(chatId: Long): Result<List<TopicModel>>

    /**
     * Returns a specific topic by chatId and topicId.
     */
    suspend fun getTopic(chatId: Long, topicId: Long): Result<TopicModel>

    /**
     * Loads topics for the given chatId, optionally from local database cache.
     */
    suspend fun loadTopics(chatId: Long, fromCache: Boolean = false): Result<Unit>

    /**
     * Reloads topics for the given chatId from remote server.
     */
    suspend fun reloadTopics(chatId: Long): Result<Unit>

    /**
     * Closes or reopens a topic.
     */
    suspend fun toggleCloseTopic(chatId: Long, topicId: Long, close: Boolean): Result<Unit>

    /**
     * Pins or unpins a topic.
     */
    suspend fun togglePinTopic(chatId: Long, topicId: Long, pin: Boolean): Result<Unit>

    /**
     * Hides or shows a topic.
     */
    suspend fun toggleShowTopic(chatId: Long, topicId: Long, show: Boolean): Result<Unit>

    /**
     * Deletes one or more topics.
     */
    suspend fun deleteTopics(chatId: Long, topicIds: List<Long>): Result<Unit>

    /**
     * Reorders pinned topics.
     */
    suspend fun reorderPinnedTopics(chatId: Long, topicIds: List<Long>): Result<Unit>

    /**
     * Marks all unread reactions in a topic as read.
     */
    suspend fun markAllReactionsAsRead(chatId: Long, topicId: Long): Result<Unit>

    /**
     * Gets current forum unread counters.
     */
    suspend fun getForumUnreadCount(chatId: Long): Result<ForumUnreadCountModel>
}
