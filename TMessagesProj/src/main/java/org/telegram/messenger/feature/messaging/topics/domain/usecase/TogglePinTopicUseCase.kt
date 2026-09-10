package org.telegram.messenger.feature.messaging.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class TogglePinTopicUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long, topicId: Long, pin: Boolean): Result<Unit> {
        return repository.togglePinTopic(chatId, topicId, pin)
    }
}
