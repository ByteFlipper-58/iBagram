package org.telegram.messenger.feature.system.windowvisibility.presentation

/**
 * MVI Events for window visibility management.
 */
sealed class WindowVisibilityEvent {
    data class HideRequested @JvmOverloads constructor(val reasonTag: String, val description: String = "") : WindowVisibilityEvent()
    data class ReleaseRequested(val reasonTag: String) : WindowVisibilityEvent()
    data class ToggleHide @JvmOverloads constructor(val reasonTag: String, val hide: Boolean, val description: String = "") : WindowVisibilityEvent()
    object ResetAll : WindowVisibilityEvent()
    object DismissError : WindowVisibilityEvent()
}
