package org.telegram.messenger.feature.messaging.topics.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.TopicsController
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.data.datasource.TopicsLocalDataSource
import org.telegram.messenger.feature.messaging.topics.data.datasource.TopicsRemoteDataSource
import org.telegram.messenger.feature.messaging.topics.data.mapper.TopicMapper
import org.telegram.messenger.feature.messaging.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.messaging.topics.domain.model.TopicModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

/**
 * Modern repository implementation for managing forum topics, coordinating local
 * cache/storage and remote MTProto sources.
 */
class TopicsRepositoryImpl(
    private val account: Int,
    private val localDataSource: TopicsLocalDataSource,
    private val remoteDataSource: TopicsRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TopicsRepository {

    override fun observeTopics(chatId: Long): Flow<List<TopicModel>> {
        return NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.topicsDidLoaded)
            .filter { event ->
                if (event.args.isEmpty()) {
                    true
                } else {
                    val eventChatId = (event.args[0] as? Number)?.toLong() ?: 0L
                    eventChatId == 0L || eventChatId == chatId || eventChatId == -chatId
                }
            }
            .map {
                getTopics(chatId).getOrDefault(emptyList())
            }
            .onStart {
                emit(getTopics(chatId).getOrDefault(emptyList()))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override fun observeForumUnreadCount(chatId: Long): Flow<ForumUnreadCountModel> {
        return NotificationCenterFlowBridge.observeEvent(account, NotificationCenter.topicsDidLoaded)
            .map {
                getForumUnreadCount(chatId).getOrDefault(ForumUnreadCountModel(chatId, 0, 0))
            }
            .onStart {
                emit(getForumUnreadCount(chatId).getOrDefault(ForumUnreadCountModel(chatId, 0, 0)))
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getTopics(chatId: Long): Result<List<TopicModel>> = withContext(mainDispatcher) {
        try {
            val topics = localDataSource.getTopics(chatId)
            val domainList = TopicMapper.toDomainList(chatId, topics)
            Result.Success(domainList)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to get topics for chat $chatId", e))
        }
    }

    override suspend fun getTopic(chatId: Long, topicId: Long): Result<TopicModel> = withContext(mainDispatcher) {
        try {
            val topic = localDataSource.findTopic(chatId, topicId)
            val domainTopic = TopicMapper.toDomain(chatId, topic)
            if (domainTopic != null) {
                Result.Success(domainTopic)
            } else {
                Result.Failure(AppError.NotFound("Topic $topicId not found in chat $chatId"))
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to find topic $topicId in chat $chatId", e))
        }
    }

    override suspend fun loadTopics(chatId: Long, fromCache: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            val loadType = if (fromCache) TopicsController.LOAD_TYPE_PRELOAD else TopicsController.LOAD_TYPE_LOAD_NEXT
            localDataSource.loadTopics(chatId, fromCache, loadType)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to load topics for chat $chatId", e))
        }
    }

    override suspend fun reloadTopics(chatId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.reloadTopics(chatId)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to reload topics for chat $chatId", e))
        }
    }

    override suspend fun toggleCloseTopic(chatId: Long, topicId: Long, close: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.toggleCloseTopic(chatId, topicId.toInt(), close)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle close topic $topicId", e))
        }
    }

    override suspend fun togglePinTopic(chatId: Long, topicId: Long, pin: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.pinTopic(chatId, topicId.toInt(), pin)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle pin topic $topicId", e))
        }
    }

    override suspend fun toggleShowTopic(chatId: Long, topicId: Long, show: Boolean): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.toggleShowTopic(chatId, topicId.toInt(), show)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle show topic $topicId", e))
        }
    }

    override suspend fun deleteTopics(chatId: Long, topicIds: List<Long>): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.deleteTopics(chatId, topicIds)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to delete topics", e))
        }
    }

    override suspend fun reorderPinnedTopics(chatId: Long, topicIds: List<Long>): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.reorderPinnedTopics(chatId, topicIds)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to reorder pinned topics", e))
        }
    }

    override suspend fun markAllReactionsAsRead(chatId: Long, topicId: Long): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.markAllReactionsAsRead(chatId, topicId)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to mark reactions as read", e))
        }
    }

    override suspend fun getForumUnreadCount(chatId: Long): Result<ForumUnreadCountModel> = withContext(mainDispatcher) {
        try {
            val counts = localDataSource.getForumUnreadCount(chatId)
            if (counts != null && counts.size >= 2) {
                Result.Success(ForumUnreadCountModel(chatId, counts[0], counts[1]))
            } else {
                Result.Success(ForumUnreadCountModel(chatId, 0, 0))
            }
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to get forum unread count for chat $chatId", e))
        }
    }
}
