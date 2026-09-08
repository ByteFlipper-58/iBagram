package org.telegram.messenger.feature.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository

class GetAvailableReactionsUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(): Result<List<ReactionItemModel>> {
        return repository.getAvailableReactions()
    }
}
