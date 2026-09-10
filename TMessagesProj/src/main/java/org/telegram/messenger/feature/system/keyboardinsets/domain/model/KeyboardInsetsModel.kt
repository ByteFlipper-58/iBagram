package org.telegram.messenger.feature.system.keyboardinsets.domain.model

enum class KeyboardVisibilityState {
    FULLY_HIDDEN,
    ANIMATING_TO_FULLY_HIDDEN,
    ANIMATING_TO_FULLY_VISIBLE,
    FULLY_VISIBLE
}

enum class InAppImeMode(val code: Int) {
    HIDDEN(0),
    VISIBLE(1),
    HIDE_AFTER_ANIMATION_END(2),
    HIDE_AFTER_KEYBOARD_OPEN(3);

    companion object {
        fun fromCode(code: Int): InAppImeMode {
            return values().firstOrNull { it.code == code } ?: HIDDEN
        }
    }
}

data class KeyboardInsetsModel(
    val inAppKeyboardHeight: Int = 0,
    val navigationBarHeight: Int = 0,
    val systemBarsTop: Int = 0,
    val systemBarsBottom: Int = 0,
    val imeBottom: Int = 0,
    val keyboardVisibility: Float = 0f,
    val keyboardState: KeyboardVisibilityState = KeyboardVisibilityState.FULLY_HIDDEN,
    val inAppImeMode: InAppImeMode = InAppImeMode.HIDDEN
) {
    val isKeyboardVisible: Boolean
        get() = keyboardVisibility > 0f || keyboardState == KeyboardVisibilityState.FULLY_VISIBLE || imeBottom > 0

    val effectiveBottomInset: Int
        get() = maxOf(imeBottom, inAppKeyboardHeight, systemBarsBottom)

    companion object {
        val DEFAULT = KeyboardInsetsModel()
    }
}
