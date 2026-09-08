package org.telegram.messenger.feature.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository

class GetDoubleTapReactionUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(): Result<String?> {
        return repository.getDoubleTapReaction()
    }
}
