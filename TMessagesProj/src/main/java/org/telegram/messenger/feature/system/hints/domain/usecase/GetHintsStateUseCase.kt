package org.telegram.messenger.feature.system.hints.domain.usecase

import org.telegram.messenger.feature.system.hints.domain.model.HintsStateModel
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository

class GetHintsStateUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(): HintsStateModel {
        return repository.getHintsState()
    }
}
