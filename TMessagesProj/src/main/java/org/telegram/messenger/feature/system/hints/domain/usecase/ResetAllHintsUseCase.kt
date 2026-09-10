package org.telegram.messenger.feature.system.hints.domain.usecase

import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository

class ResetAllHintsUseCase(
    private val repository: HintsRepository
) {
    operator fun invoke() {
        repository.resetAllHints()
    }
}
