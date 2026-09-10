package org.telegram.messenger.feature.system.pinchtozoom.presentation

import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchZoomState

/**
 * Pure reactive UI state for PinchToZoom presentation.
 */
data class PinchToZoomUiState(
    val zoomState: PinchZoomState = PinchZoomState(),
    val currentTransform: PinchTransform? = null,
    val isGestureActive: Boolean = false
)
