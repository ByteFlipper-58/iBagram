package org.telegram.messenger.feature.fpscontent.presentation

/**
 * MVI Events for FPS arbitration management.
 */
sealed class FpsContentEvent {
    data class RegisterFrameCallback(val fps: Int = 60, val isOneShot: Boolean = false) : FpsContentEvent()
    data class RegisterRunnableCallback(val fps: Int = 60, val isOneShot: Boolean = false) : FpsContentEvent()
    data class UnregisterCallback(val subscriptionId: String) : FpsContentEvent()
    data class PostInvalidateView(val viewId: String) : FpsContentEvent()
    data class PostInvalidateDrawable(val drawableId: String, val fps: Int = 60) : FpsContentEvent()
    data class DispatchVsync(val frameTimeNanos: Long) : FpsContentEvent()
    object Reset : FpsContentEvent()
    object DismissError : FpsContentEvent()
}
