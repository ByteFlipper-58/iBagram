package org.telegram.messenger.feature.messaging.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

class GetDoubleTapReactionUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(): Result<String?> {
        return repository.getDoubleTapReaction()
    }
}
