package org.telegram.messenger.feature.pinchtozoom.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchZoomState

/**
 * Domain contract for interactive pinch-to-zoom gestures and transformations.
 */
interface PinchToZoomRepository {

    fun observeState(): StateFlow<PinchZoomState>

    fun getState(): PinchZoomState

    fun calculateScale(currentDistance: Float, startDistance: Float): Float

    fun calculateTranslation(
        startCenterX: Float,
        startCenterY: Float,
        currentCenterX: Float,
        currentCenterY: Float,
        scale: Float
    ): Pair<Float, Float>

    fun calculateTransform(
        scale: Float,
        finishProgress: Float,
        parentOffsetX: Float,
        parentOffsetY: Float,
        pinchCenterX: Float,
        pinchCenterY: Float,
        translationX: Float,
        translationY: Float
    ): PinchTransform

    fun calculateImageBounds(dimensions: PinchImageDimensions, scale: Float): PinchBoundsResult

    fun evaluatePinchGesture(
        startDistance: Float,
        startCenterX: Float,
        startCenterY: Float,
        point1: PinchTouchPoint,
        point2: PinchTouchPoint,
        isInOverlay: Boolean
    ): PinchGestureDecision

    fun startZoom(scale: Float = 1f)

    fun updateZoom(scale: Float, translationX: Float, translationY: Float)

    fun finishZoom(progress: Float)

    fun reset()
}
