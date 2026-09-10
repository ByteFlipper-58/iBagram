package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class UpdateAdjustPanTransitionUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(progress: Float) {
        repository.updateTransition(progress)
    }
}
