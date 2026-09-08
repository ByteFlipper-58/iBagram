package org.telegram.messenger.feature.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository

class MarkTopicReactionsAsReadUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long, topicId: Long): Result<Unit> {
        return repository.markAllReactionsAsRead(chatId, topicId)
    }
}
