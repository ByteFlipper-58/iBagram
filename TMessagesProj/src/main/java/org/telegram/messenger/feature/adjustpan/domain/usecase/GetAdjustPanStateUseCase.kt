package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionState
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class GetAdjustPanStateUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(): PanTransitionState {
        return repository.getState()
    }
}
