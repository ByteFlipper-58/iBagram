package org.telegram.messenger.feature.adjustpan.domain.usecase

import org.telegram.messenger.feature.adjustpan.domain.model.PanProgressResult
import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository

class ComputePanProgressUseCase(
    private val repository: AdjustPanRepository
) {
    operator fun invoke(plan: PanTransitionPlan, progress: Float): PanProgressResult {
        return repository.computeProgress(plan, progress)
    }
}
