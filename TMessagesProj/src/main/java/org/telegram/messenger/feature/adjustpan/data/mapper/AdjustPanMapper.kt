package org.telegram.messenger.feature.adjustpan.data.mapper

import org.telegram.messenger.feature.adjustpan.domain.model.PanCalculationSpec
import org.telegram.messenger.feature.adjustpan.domain.model.PanProgressResult
import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionPlan
import kotlin.math.abs

/**
 * Pure mapper and calculation engine for AdjustPan layout geometry.
 */
object AdjustPanMapper {

    /**
     * Determines whether height adjustment transition should be triggered.
     */
    fun shouldAnimate(spec: PanCalculationSpec): Boolean {
        if (!spec.isHeightAnimationEnabled) return false
        if (spec.previousHeight == -1) return false
        val heightDelta = abs(spec.previousHeight - spec.contentHeight)
        if (heightDelta < spec.thresholdPx) return false
        return true
    }

    /**
     * Calculates the complete transition plan based on geometry specs.
     */
    fun calculatePlan(spec: PanCalculationSpec): PanTransitionPlan {
        if (!shouldAnimate(spec)) {
            return PanTransitionPlan.NO_ANIMATION
        }

        val isKeyboardVisible = if (spec.contentViewBottom > 0) {
            spec.contentHeight < spec.contentViewBottom
        } else {
            spec.contentHeight < spec.previousHeight
        }

        val showingKeyboard = spec.contentHeight <= spec.previousHeight
        val dy = (spec.contentHeight - spec.previousHeight).toFloat()
        val keyboardSize = abs(dy)
        val targetHeight = maxOf(
            spec.previousHeight,
            spec.contentHeight + spec.additionalContentHeight + spec.bottomTabsHeight
        )

        val fromY: Float
        val toY: Float
        val inverse: Boolean

        if (spec.contentHeight > spec.previousHeight) {
            val adjustedDy = dy - spec.startOffset
            fromY = -adjustedDy
            toY = -spec.bottomTabsHeight.toFloat()
            inverse = true
        } else {
            toY = -spec.previousStartOffset.toFloat()
            fromY = dy
            inverse = false
        }

        return PanTransitionPlan(
            shouldAnimate = true,
            isKeyboardVisible = isKeyboardVisible,
            showingKeyboard = showingKeyboard,
            keyboardSize = keyboardSize,
            targetHeight = targetHeight,
            fromY = fromY,
            toY = toY,
            inverse = inverse
        )
    }

    /**
     * Calculates interpolated translation position along the trajectory.
     */
    fun interpolateProgress(plan: PanTransitionPlan, progress: Float): PanProgressResult {
        if (!plan.shouldAnimate) {
            return PanProgressResult(
                translationY = 0f,
                panProgress = progress,
                isKeyboardVisible = plan.isKeyboardVisible
            )
        }

        var t = progress.coerceIn(0f, 1f)
        if (plan.inverse) {
            t = 1f - t
        }

        val y = (plan.fromY * t + plan.toY * (1f - t)).toInt().toFloat()
        return PanProgressResult(
            translationY = y,
            panProgress = progress,
            isKeyboardVisible = plan.isKeyboardVisible
        )
    }
}
