package org.telegram.messenger.feature.recyclerscroll.domain.usecase

import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository

class EvaluateScrollEligibilityUseCase(
    private val repository: RecyclerScrollRepository
) {
    operator fun invoke(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility = repository.evaluateEligibility(
        fastScrollRunning = fastScrollRunning,
        itemAnimatorRunning = itemAnimatorRunning,
        childCount = childCount,
        viewAnimationsEnabled = viewAnimationsEnabled,
        smooth = smooth,
        direction = direction
    )
}
