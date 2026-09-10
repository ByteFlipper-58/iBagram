package org.telegram.messenger.feature.messaging.bottomviews.presentation

sealed class BottomViewsEvent {
    data class SetViewVisible(
        val containerId: Int,
        val isVisible: Boolean,
        val animated: Boolean = true
    ) : BottomViewsEvent()

    object ResetToDefault : BottomViewsEvent()
}
