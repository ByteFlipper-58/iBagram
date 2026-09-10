package org.telegram.messenger.feature.system.adjustpan.domain.usecase

import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository

class ResetAdjustPanUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
