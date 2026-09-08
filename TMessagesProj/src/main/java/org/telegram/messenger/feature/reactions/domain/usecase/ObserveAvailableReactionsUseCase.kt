package org.telegram.messenger.feature.reactions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository

class ObserveAvailableReactionsUseCase(
    private val repository: ReactionsRepository
) {
    operator fun invoke(): Flow<List<ReactionItemModel>> {
        return repository.observeAvailableReactions()
    }
}
