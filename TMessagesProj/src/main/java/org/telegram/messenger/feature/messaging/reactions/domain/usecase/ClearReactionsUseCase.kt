package org.telegram.messenger.feature.messaging.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

class ClearReactionsUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(dialogId: Long, messageId: Int): Result<Unit> {
        return repository.clearReactions(dialogId, messageId)
    }
}
