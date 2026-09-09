package org.telegram.messenger.feature.hints.domain.usecase

import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository

class ResetHintUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(type: HintType) {
        repository.resetHint(type)
    }
}
