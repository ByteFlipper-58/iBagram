package org.telegram.messenger.feature.system.recyclerscroll.data.mapper

import kotlin.math.abs
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationPlan
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollAnimationSpec
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollDirection
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollEligibility
import org.telegram.messenger.feature.system.recyclerscroll.domain.model.ScrollViewTranslation

/**
 * Pure mathematical formulas and logic for recycler list scroll animations.
 */
object RecyclerScrollMapper {

    fun evaluateEligibility(
        fastScrollRunning: Boolean,
        itemAnimatorRunning: Boolean,
        childCount: Int,
        viewAnimationsEnabled: Boolean,
        smooth: Boolean,
        direction: ScrollDirection
    ): ScrollEligibility {
        if (fastScrollRunning || itemAnimatorRunning) {
            return ScrollEligibility(canAnimate = false, fallbackToInstant = false)
        }
        if (!smooth || direction == ScrollDirection.UNSET || childCount == 0 || !viewAnimationsEnabled) {
            return ScrollEligibility(canAnimate = false, fallbackToInstant = true)
        }
        return ScrollEligibility(canAnimate = true, fallbackToInstant = false)
    }

    fun calculatePlan(spec: ScrollAnimationSpec): ScrollAnimationPlan {
        val isScrollDown = spec.scrollDirection == ScrollDirection.DOWN
        val duration: Long = if (spec.isDialogs) {
            if (spec.hasSameViews) {
                150L
            } else {
                val height = if (spec.containerHeight <= 0) 1 else spec.containerHeight
                val d = (((spec.scrollLength / height.toFloat()) + 1f) * 200L).toLong()
                d.coerceIn(300L, 1300L)
            }
        } else {
            if (spec.hasSameViews) {
                600L
            } else {
                val height = if (spec.containerHeight <= 0) 1 else spec.containerHeight
                val d = (((spec.scrollLength / height.toFloat()) + 1f) * 200L).toLong()
                d.coerceIn(300L, 1300L)
            }
        }
        return ScrollAnimationPlan(
            durationMs = duration,
            scrollLength = spec.scrollLength,
            isScrollDown = isScrollDown
        )
    }

    fun calculateScrollLength(
        scrollDown: Boolean,
        containerHeight: Int,
        oldViewsCount: Int,
        scrollDiff: Int,
        oldTop: Int,
        oldBottom: Int,
        incomingTop: Int,
        incomingBottom: Int
    ): Int {
        if (oldViewsCount == 0) {
            return abs(scrollDiff)
        }
        val finalHeight = if (scrollDown) oldBottom else containerHeight - oldTop
        val offset = if (scrollDown) -incomingTop else (incomingBottom - containerHeight)
        return finalHeight + offset
    }

    fun computeViewTranslations(
        scrollLength: Int,
        isScrollDown: Boolean,
        progress: Float,
        additionalY: Int = 0
    ): ScrollViewTranslation {
        val clampedProgress = progress.coerceIn(0f, 1f)
        val oldTranslation = if (isScrollDown) {
            -scrollLength * clampedProgress
        } else {
            scrollLength * clampedProgress
        }
        val incomingTranslation = if (isScrollDown) {
            scrollLength * (1f - clampedProgress) + additionalY
        } else {
            -scrollLength * (1f - clampedProgress)
        }
        return ScrollViewTranslation(
            oldViewsTranslationY = oldTranslation,
            incomingViewsTranslationY = incomingTranslation
        )
    }
}
