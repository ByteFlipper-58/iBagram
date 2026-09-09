package org.telegram.messenger.feature.hints.domain.usecase

import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository

class ShouldShowHintUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke(type: HintType): Boolean {
        return repository.shouldShowHint(type)
    }
}
