package org.telegram.messenger.feature.pinchtozoom.presentation

import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchZoomState

/**
 * Pure reactive UI state for PinchToZoom presentation.
 */
data class PinchToZoomUiState(
    val zoomState: PinchZoomState = PinchZoomState(),
    val currentTransform: PinchTransform? = null,
    val isGestureActive: Boolean = false
)
