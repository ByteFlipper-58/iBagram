package org.telegram.messenger.feature.system.pinchtozoom.presentation

import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTouchPoint

/**
 * MVI events for PinchToZoom interactions.
 */
sealed interface PinchToZoomEvent {
    data class StartGesture(
        val point1: PinchTouchPoint,
        val point2: PinchTouchPoint
    ) : PinchToZoomEvent

    data class UpdateGesture(
        val point1: PinchTouchPoint,
        val point2: PinchTouchPoint
    ) : PinchToZoomEvent

    data class StartZoom(val scale: Float = 1f) : PinchToZoomEvent

    data class UpdateZoom(
        val scale: Float,
        val translationX: Float,
        val translationY: Float
    ) : PinchToZoomEvent

    data class FinishZoom(val progress: Float) : PinchToZoomEvent

    data object EndGesture : PinchToZoomEvent

    data object Reset : PinchToZoomEvent
}
