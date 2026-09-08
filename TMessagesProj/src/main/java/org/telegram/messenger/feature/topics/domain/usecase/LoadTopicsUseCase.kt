package org.telegram.messenger.feature.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository

class LoadTopicsUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long, fromCache: Boolean = false): Result<Unit> {
        return repository.loadTopics(chatId, fromCache)
    }
}
