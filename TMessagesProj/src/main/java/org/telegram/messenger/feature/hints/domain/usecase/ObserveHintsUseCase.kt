package org.telegram.messenger.feature.hints.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository

class ObserveHintsUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(): Flow<HintsStateModel> {
        return repository.observeHints()
    }
}
