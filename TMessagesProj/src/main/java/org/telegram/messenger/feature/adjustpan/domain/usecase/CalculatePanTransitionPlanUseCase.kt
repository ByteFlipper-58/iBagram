package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class CalculatePanTransitionPlanUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(spec: PanCalculationSpec): PanTransitionPlan {
        return repository.calculatePlan(spec)
    }
}
