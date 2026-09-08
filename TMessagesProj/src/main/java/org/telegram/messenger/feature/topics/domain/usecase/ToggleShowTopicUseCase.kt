package org.telegram.messenger.feature.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository

class ToggleShowTopicUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long, topicId: Long, show: Boolean): Result<Unit> {
        return repository.toggleShowTopic(chatId, topicId, show)
    }
}
