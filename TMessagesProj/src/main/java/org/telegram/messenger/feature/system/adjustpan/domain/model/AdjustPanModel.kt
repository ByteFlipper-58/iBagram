package org.telegram.messenger.feature.system.adjustpan.domain.model

/**
 * Input specification for calculating adjust pan geometry and transition parameters.
 */
data class PanCalculationSpec(
    val previousHeight: Int,
    val contentHeight: Int,
    val previousStartOffset: Int = 0,
    val startOffset: Int = 0,
    val contentViewBottom: Int = 0,
    val additionalContentHeight: Int = 0,
    val bottomTabsHeight: Int = 0,
    val thresholdPx: Int = 0,
    val isHeightAnimationEnabled: Boolean = true
)

/**
 * Calculated transition plan representing the geometric animation trajectory.
 */
data class PanTransitionPlan(
    val shouldAnimate: Boolean,
    val isKeyboardVisible: Boolean,
    val showingKeyboard: Boolean,
    val keyboardSize: Float,
    val targetHeight: Int,
    val fromY: Float,
    val toY: Float,
    val inverse: Boolean
) {
    companion object {
        val NO_ANIMATION = PanTransitionPlan(
            shouldAnimate = false,
            isKeyboardVisible = false,
            showingKeyboard = false,
            keyboardSize = 0f,
            targetHeight = -1,
            fromY = 0f,
            toY = 0f,
            inverse = false
        )
    }
}

/**
 * Current interpolated translation result during an adjust-pan step.
 */
data class PanProgressResult(
    val translationY: Float,
    val panProgress: Float,
    val isKeyboardVisible: Boolean
)

/**
 * Snapshot state of the adjust-pan controller.
 */
data class PanTransitionState(
    val isEnabled: Boolean = true,
    val isAnimationInProgress: Boolean = false,
    val isShowingKeyboard: Boolean = false,
    val isKeyboardVisible: Boolean = false,
    val currentTranslationY: Float = 0f,
    val currentProgress: Float = 0f,
    val keyboardSize: Float = 0f,
    val targetHeight: Int = -1
)
