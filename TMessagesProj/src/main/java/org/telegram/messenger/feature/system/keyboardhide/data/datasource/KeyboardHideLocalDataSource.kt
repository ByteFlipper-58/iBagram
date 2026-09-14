package org.telegram.messenger.feature.system.keyboardhide.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.system.keyboardhide.data.mapper.KeyboardHideMapper
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideState
import org.telegram.ui.KeyboardHideHelper

open class KeyboardHideLocalDataSource(
    initialEnabled: Boolean? = null,
    private val testMode: Boolean = false
) {
    private val _state: MutableStateFlow<KeyboardHideState>

    init {
        val enabled = initialEnabled ?: if (testMode) {
            false
        } else {
            try {
                KeyboardHideHelper.ENABLED
            } catch (_: Throwable) {
                false
            }
        }
        _state = MutableStateFlow(KeyboardHideState(isEnabled = enabled))
    }

    open fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        return KeyboardHideMapper.calculateProgress(spec)
    }

    open fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float = 0f
    ): KeyboardDismissDecision {
        return KeyboardHideMapper.evaluateDismissDecision(currentProgress, lastDifferentProgress, velocityY)
    }

    open fun observeState(): StateFlow<KeyboardHideState> = _state.asStateFlow()

    open fun getState(): KeyboardHideState = _state.value

    open fun setEnabled(enabled: Boolean) {
        if (!testMode) {
            try {
                KeyboardHideHelper.ENABLED = enabled
            } catch (_: Throwable) {
                // Headless JVM fallback
            }
        }
        _state.update { it.copy(isEnabled = enabled) }
    }

    open fun startMoving(keyboardSize: Int, bottomNavBarSize: Int, isKeyboard: Boolean) {
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

    open fun updateMoving(rawProgress: Float, progress: Float, translationY: Float) {
        _state.update {
            it.copy(
                rawProgress = rawProgress,
                currentProgress = progress,
                translationY = translationY
            )
        }
    }

    open fun endMoving(shouldDismiss: Boolean) {
        _state.update {
            it.copy(
                isMovingKeyboard = false,
                isEndingMovingKeyboard = true
            )
        }
    }

    open fun finishDismiss(dismissed: Boolean) {
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

    open fun reset() {
        _state.value = KeyboardHideState(isEnabled = _state.value.isEnabled)
    }
}
