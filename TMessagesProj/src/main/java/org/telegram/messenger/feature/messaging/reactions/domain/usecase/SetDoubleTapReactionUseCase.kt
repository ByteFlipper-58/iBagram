package org.telegram.messenger.feature.messaging.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

class SetDoubleTapReactionUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(reaction: String): Result<Unit> {
        return repository.setDoubleTapReaction(reaction)
    }
}
