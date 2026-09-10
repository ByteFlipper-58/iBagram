package org.telegram.messenger.feature.system.adjustpan.domain.usecase

import org.telegram.messenger.feature.system.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository

class CalculatePanTransitionPlanUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(spec: PanCalculationSpec): PanTransitionPlan {
        return repository.calculatePlan(spec)
    }
}
