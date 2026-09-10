package org.telegram.messenger.feature.keyboardhide.presentation

sealed interface KeyboardHideEvent {
    data class SetEnabled(val enabled: Boolean) : KeyboardHideEvent
    data class StartDrag(val keyboardSize: Int, val bottomNavBarSize: Int, val isKeyboard: Boolean) : KeyboardHideEvent
    data class UpdateDrag(val rawProgress: Float, val progress: Float, val translationY: Float) : KeyboardHideEvent
    data class EndDrag(val shouldDismiss: Boolean) : KeyboardHideEvent
    data class FinishDismiss(val dismissed: Boolean) : KeyboardHideEvent
    object Reset : KeyboardHideEvent
}
