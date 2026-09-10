package org.telegram.messenger.feature.messaging.reactions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

class ObserveRecentReactionsUseCase(
    private val repository: ReactionsRepository
) {
    operator fun invoke(): Flow<List<ReactionItemModel>> {
        return repository.observeRecentReactions()
    }
}
