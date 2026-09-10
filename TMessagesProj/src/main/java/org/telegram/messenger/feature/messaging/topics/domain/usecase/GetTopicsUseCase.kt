package org.telegram.messenger.feature.messaging.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.domain.model.TopicModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class GetTopicsUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long): Result<List<TopicModel>> {
        return repository.getTopics(chatId)
    }
}
