package org.telegram.messenger.feature.chat.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chat.domain.repository.ChatRepository

/**
 * Use case to load older messages in a chat dialog for pagination.
 */
class LoadHistoryUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(dialogId: Long, count: Int = 30): Result<Unit> {
        return repository.loadHistory(dialogId, count)
    }
}
