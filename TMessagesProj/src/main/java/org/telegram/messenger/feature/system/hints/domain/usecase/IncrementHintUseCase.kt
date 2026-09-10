package org.telegram.messenger.feature.system.hints.domain.usecase

import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository

class IncrementHintUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(type: HintType) {
        repository.incrementHint(type)
    }
}
