package org.telegram.messenger.feature.keyboardhide.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.keyboardhide.data.mapper.KeyboardHideMapper
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideState
import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository
import org.telegram.ui.KeyboardHideHelper

/**
 * Thread-safe adapter implementation for KeyboardHide gesture tracking and scroll arbitration.
 */
class LegacyKeyboardHideRepository(
    initialEnabled: Boolean? = null
) : KeyboardHideRepository {

    private val _state: MutableStateFlow<KeyboardHideState>

    init {
        val enabled = initialEnabled ?: try {
            KeyboardHideHelper.ENABLED
        } catch (_: Throwable) {
            false
        }
        _state = MutableStateFlow(KeyboardHideState(isEnabled = enabled))
    }

    override fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        return KeyboardHideMapper.calculateProgress(spec)
    }

    override fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float
    ): KeyboardDismissDecision {
        return KeyboardHideMapper.evaluateDismissDecision(currentProgress, lastDifferentProgress, velocityY)
    }

    override fun observeState(): StateFlow<KeyboardHideState> = _state.asStateFlow()

    override fun getState(): KeyboardHideState = _state.value

    override fun setEnabled(enabled: Boolean) {
        try {
            KeyboardHideHelper.ENABLED = enabled
        } catch (_: Throwable) {
            // Headless JVM fallback
        }
        _state.update { it.copy(isEnabled = enabled) }
    }

    override fun startMoving(keyboardSize: Int, bottomNavBarSize: Int, isKeyboard: Boolean) {
        _state.update {
            it.copy(
                isMovingKeyboard = true,
                isEndingMovingKeyboard = false,
                isKeyboard = isKeyboard,
                keyboardSize = keyboardSize,
                bottomNavBarSize = bottomNavBarSize,
                rawProgress = 0f,
                currentProgress = 0f,
                translationY = 0f
            )
        }
    }

    override fun updateMoving(rawProgress: Float, progress: Float, translationY: Float) {
        _state.update {
            it.copy(
                rawProgress = rawProgress,
                currentProgress = progress,
                translationY = translationY
            )
        }
    }

    override fun endMoving(shouldDismiss: Boolean) {
        _state.update {
            it.copy(
                isMovingKeyboard = false,
                isEndingMovingKeyboard = true
            )
        }
    }

    override fun finishDismiss(dismissed: Boolean) {
        _state.update {
            it.copy(
                isMovingKeyboard = false,
                isEndingMovingKeyboard = false,
                rawProgress = if (dismissed) 1f else 0f,
                currentProgress = if (dismissed) 1f else 0f,
                translationY = 0f
            )
        }
    }

    override fun reset() {
        _state.value = KeyboardHideState(isEnabled = _state.value.isEnabled)
    }
}
