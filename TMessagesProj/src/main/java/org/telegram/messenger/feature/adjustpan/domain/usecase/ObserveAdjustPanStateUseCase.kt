package org.telegram.messenger.feature.adjustpan.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionState
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class ObserveAdjustPanStateUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(): StateFlow<PanTransitionState> {
        return repository.observeState()
    }
}
