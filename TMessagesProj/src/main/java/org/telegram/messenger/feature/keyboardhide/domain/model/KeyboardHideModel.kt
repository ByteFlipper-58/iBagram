package org.telegram.messenger.feature.keyboardhide.domain.model

/**
 * Input specifications for drag gesture calculations during keyboard pull-down dismissal.
 */
data class KeyboardDragSpec(
    val fromY: Float,
    val currentRawY: Float,
    val keyboardSize: Int,
    val bottomNavBarSize: Int = 0,
    val isKeyboard: Boolean = true
)

/**
 * Evaluated dismissal decision indicating whether keyboard should finish closing or bounce back.
 */
data class KeyboardDismissDecision(
    val shouldDismiss: Boolean,
    val targetProgress: Float
)

/**
 * Calculated progress and geometry metrics during drag or dismissal animation.
 */
data class KeyboardHideProgressResult(
    val rawProgress: Float,
    val clampedProgress: Float,
    val translationY: Float,
    val insetHeight: Int,
    val alpha: Float
)

/**
 * Reactive snapshot state of the keyboard hide interactive session.
 */
data class KeyboardHideState(
    val isEnabled: Boolean = false,
    val isMovingKeyboard: Boolean = false,
    val isEndingMovingKeyboard: Boolean = false,
    val isKeyboard: Boolean = false,
    val rawProgress: Float = 0f,
    val currentProgress: Float = 0f,
    val translationY: Float = 0f,
    val keyboardSize: Int = 0,
    val bottomNavBarSize: Int = 0
) {
    val disableScrolling: Boolean
        get() = isEnabled && (isMovingKeyboard || isEndingMovingKeyboard) && rawProgress >= 0f
}
