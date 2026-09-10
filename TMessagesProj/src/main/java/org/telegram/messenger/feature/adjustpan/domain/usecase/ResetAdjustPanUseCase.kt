package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class ResetAdjustPanUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
