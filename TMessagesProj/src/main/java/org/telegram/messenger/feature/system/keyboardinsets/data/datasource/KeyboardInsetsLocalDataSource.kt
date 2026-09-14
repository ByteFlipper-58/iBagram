package org.telegram.messenger.feature.system.keyboardinsets.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.system.keyboardinsets.data.mapper.KeyboardInsetsMapper
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.InAppImeMode
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardVisibilityState
import org.telegram.ui.Components.inset.WindowInsetsInAppController

open class KeyboardInsetsLocalDataSource(
    private val inAppController: WindowInsetsInAppController? = null
) {
    private val lock = Any()
    private var inAppHeight: Int = 0
    private var navBarHeight: Int = 0
    private var barsTop: Int = 0
    private var barsBottom: Int = 0
    private var imeInsetBottom: Int = 0
    private var visibilityProgress: Float = 0f
    private var visibilityState: KeyboardVisibilityState = KeyboardVisibilityState.FULLY_HIDDEN
    private var imeMode: InAppImeMode = InAppImeMode.HIDDEN

    private val _stateFlow = MutableStateFlow(KeyboardInsetsModel.DEFAULT)

    open fun requestInAppKeyboardHeight(height: Int) {
        synchronized(lock) {
            inAppHeight = height
            imeMode = if (height > 0) InAppImeMode.VISIBLE else InAppImeMode.HIDDEN
            try {
                inAppController?.requestInAppKeyboardHeight(height)
            } catch (_: Throwable) {
                // Ignore failure in headless test environment
            }
            emitStateLocked()
        }
    }

    open fun resetInAppKeyboardHeight(waitKeyboardOpen: Boolean) {
        synchronized(lock) {
            inAppHeight = 0
            imeMode = if (waitKeyboardOpen) InAppImeMode.HIDE_AFTER_KEYBOARD_OPEN else InAppImeMode.HIDDEN
            try {
                inAppController?.resetInAppKeyboardHeight(waitKeyboardOpen)
            } catch (_: Throwable) {
                // Ignore failure in headless test environment
            }
            emitStateLocked()
        }
    }

    open fun requestInAppKeyboardHeightIncludeNavbar(height: Int, navigationBarHeight: Int) {
        synchronized(lock) {
            navBarHeight = navigationBarHeight
            if (height > 0) {
                requestInAppKeyboardHeight(height + navigationBarHeight)
            } else {
                resetInAppKeyboardHeight(true)
            }
        }
    }

    open fun updateSystemInsets(top: Int, bottom: Int, imeBottom: Int, animated: Boolean) {
        synchronized(lock) {
            barsTop = top
            barsBottom = bottom
            imeInsetBottom = imeBottom

            if (imeBottom > 0) {
                visibilityState = if (animated) KeyboardVisibilityState.ANIMATING_TO_FULLY_VISIBLE else KeyboardVisibilityState.FULLY_VISIBLE
                visibilityProgress = 1.0f
            } else {
                visibilityState = if (animated) KeyboardVisibilityState.ANIMATING_TO_FULLY_HIDDEN else KeyboardVisibilityState.FULLY_HIDDEN
                visibilityProgress = 0.0f
            }
            emitStateLocked()
        }
    }

    open fun getKeyboardInsets(): KeyboardInsetsModel {
        synchronized(lock) {
            return _stateFlow.value
        }
    }

    open fun observeKeyboardInsets(): Flow<KeyboardInsetsModel> {
        return _stateFlow.asStateFlow()
    }

    private fun emitStateLocked() {
        _stateFlow.value = KeyboardInsetsMapper.toModel(
            inAppKeyboardHeight = inAppHeight,
            navigationBarHeight = navBarHeight,
            systemBarsTop = barsTop,
            systemBarsBottom = barsBottom,
            imeBottom = imeInsetBottom,
            keyboardVisibility = visibilityProgress,
            keyboardState = visibilityState,
            inAppImeMode = imeMode
        )
    }
}
