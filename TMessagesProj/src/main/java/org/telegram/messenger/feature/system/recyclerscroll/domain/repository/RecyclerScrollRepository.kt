package org.telegram.messenger.feature.system.recyclerscroll.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.RecyclerScrollState
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollViewTranslation

/**
 * Domain contract for animated list scrolling and view transition math.
 */
interface RecyclerScrollRepository {

    fun observeState(): StateFlow<RecyclerScrollState>

    fun getState(): RecyclerScrollState

    fun evaluateEligibility(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility

    fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan

    fun calculateScrollLength(
        scrollDown: Boolean,
        containerHeight: Int,
        oldViewsCount: Int,
        scrollDiff: Int,
        oldTop: Int,
        oldBottom: Int,
        incomingTop: Int,
        incomingBottom: Int
    ): Int

    fun computeViewTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int = 0
    ): ScrollViewTranslation

    fun startScroll(position: Int, offset: Int, direction: ScrollDirection, durationMs: Long)

    fun updateProgress(progress: Float)

    fun finishScroll()

    fun cancelScroll()

    fun reset()
}
