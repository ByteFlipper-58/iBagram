package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class CalculateScrollAnimationPlanUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(spec: ScrollAnimationSpec): ScrollAnimationPlan =
        repository.calculatePlan(spec)
}
