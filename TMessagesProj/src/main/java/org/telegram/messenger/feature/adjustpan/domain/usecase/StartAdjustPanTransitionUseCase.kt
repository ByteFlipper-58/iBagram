package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class StartAdjustPanTransitionUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(plan: PanTransitionPlan) {
        repository.startTransition(plan)
    }
}
