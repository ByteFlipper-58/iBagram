package org.telegram.messenger.feature.messaging.topics.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.topics.domain.model.TopicModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class ObserveTopicsUseCase(
    private val repository: TopicsRepository
) {
    operator fun invoke(chatId: Long): Flow<List<TopicModel>> {
        return repository.observeTopics(chatId)
    }
}
