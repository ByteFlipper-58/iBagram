package org.telegram.messenger.feature.hints.domain.usecase

import org.telegram.messenger.feature.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository

class GetHintsStateUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(): HintsStateModel {
        return repository.getHintsState()
    }
}
