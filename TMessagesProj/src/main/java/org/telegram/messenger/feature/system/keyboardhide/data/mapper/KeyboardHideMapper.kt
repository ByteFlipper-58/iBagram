package org.telegram.messenger.feature.system.keyboardhide.data.mapper

import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideProgressResult

/**
 * Pure calculation engine for keyboard drag progress, translation, and dismiss decisions.
 */
object KeyboardHideMapper {

    /**
     * Calculates the gesture progress and translation offset.
     */
    fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        if (spec.keyboardSize <= 0) {
            return KeyboardHideProgressResult(
                rawProgress = 0f,
                clampedProgress = 0f,
                translationY = 0f,
                insetHeight = 0,
                alpha = 1f
            )
        }

        val rawProgress = (spec.currentRawY - spec.fromY) / spec.keyboardSize.toFloat()
        val clampedProgress = rawProgress.coerceIn(0f, 1f)

        val translationY = if (spec.isKeyboard) {
            maxOf(0f, (1f - clampedProgress) * spec.keyboardSize - spec.bottomNavBarSize - 1f)
        } else {
            (1f - clampedProgress) * spec.keyboardSize
        }

        val insetHeight = ((1f - clampedProgress) * spec.keyboardSize).toInt().coerceAtLeast(0)
        val alpha = clampedProgress

        return KeyboardHideProgressResult(
            rawProgress = rawProgress,
            clampedProgress = clampedProgress,
            translationY = translationY,
            insetHeight = insetHeight,
            alpha = alpha
        )
    }

    /**
     * Determines whether the gesture has crossed the threshold to dismiss the keyboard.
     * Logic matches Telegram: t > 0.15 && t >= lastDifferentT || t > 0.8 || velocityY > 1000.
     */
    fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float = 0f
    ): KeyboardDismissDecision {
        val shouldDismiss = (currentProgress > 0.15f && currentProgress >= lastDifferentProgress) ||
            currentProgress > 0.8f ||
            velocityY > 1000f

        return KeyboardDismissDecision(
            shouldDismiss = shouldDismiss,
            targetProgress = if (shouldDismiss) 1f else 0f
        )
    }
}
