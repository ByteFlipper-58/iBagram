package org.telegram.messenger.feature.messaging.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class ReorderPinnedTopicsUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long, topicIds: List<Long>): Result<Unit> {
        return repository.reorderPinnedTopics(chatId, topicIds)
    }
}
