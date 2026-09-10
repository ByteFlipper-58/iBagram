package org.telegram.messenger.feature.system.keyboardhide.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideState

/**
 * Contract for managing interactive pull-down keyboard dismissal and scroll arbitration.
 */
interface KeyboardHideRepository {

    /**
     * Calculates translation and inset geometry based on the current drag touch position.
     */
    fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult

    /**
     * Evaluates whether gesture progress and velocity warrant full dismissal or snapping back.
     */
    fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float = 0f
    ): KeyboardDismissDecision

    /**
     * Observes real-time state changes of the keyboard hide controller.
     */
    fun observeState(): StateFlow<KeyboardHideState>

    /**
     * Returns the current snapshot state.
     */
    fun getState(): KeyboardHideState

    /**
     * Enables or disables pull-to-hide functionality.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Signals initiation of an interactive drag gesture.
     */
    fun startMoving(keyboardSize: Int, bottomNavBarSize: Int, isKeyboard: Boolean)

    /**
     * Updates ongoing drag progress and offset.
     */
    fun updateMoving(rawProgress: Float, progress: Float, translationY: Float)

    /**
     * Marks the transition from user dragging to settling animation.
     */
    fun endMoving(shouldDismiss: Boolean)

    /**
     * Concludes the dismissal lifecycle.
     */
    fun finishDismiss(dismissed: Boolean)

    /**
     * Resets repository state to default.
     */
    fun reset()
}
