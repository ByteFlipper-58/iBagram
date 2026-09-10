package org.telegram.messenger.feature.pinchtozoom.domain.model

import kotlin.math.hypot

/**
 * Pure model representing a touch point in pinch-to-zoom gestures.
 */
data class PinchTouchPoint(
    val x: Float,
    val y: Float,
    val id: Int = 0
)

/**
 * Pure domain model representing the pair of pointers in a pinch gesture.
 */
data class PinchGestureSpec(
    val point1: PinchTouchPoint,
    val point2: PinchTouchPoint
) {
    val distance: Float
        get() = hypot(point2.x - point1.x, point2.y - point1.y)

    val centerX: Float
        get() = (point1.x + point2.x) / 2.0f

    val centerY: Float
        get() = (point1.y + point2.y) / 2.0f
}

/**
 * Evaluated decision for an active pinch gesture.
 */
data class PinchGestureDecision(
    val shouldStartZoom: Boolean,
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val centerX: Float,
    val centerY: Float
)

/**
 * Calculated 2D canvas transform for pinch overlay rendering.
 */
data class PinchTransform(
    val scale: Float,
    val pivotX: Float,
    val pivotY: Float,
    val translationX: Float,
    val translationY: Float
)

/**
 * Media dimensions and bounds for zoom aspect-ratio compensation.
 */
data class PinchImageDimensions(
    val imageX: Float,
    val imageY: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val fullImageWidth: Float,
    val fullImageHeight: Float
)

/**
 * Adjusted bounds for full-view aspect ratio transition during zoom.
 */
data class PinchBoundsResult(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val progress: Float
)

/**
 * Reactive state of the pinch-to-zoom subsystem.
 */
data class PinchZoomState(
    val isInOverlayMode: Boolean = false,
    val isInTouchMode: Boolean = false,
    val scale: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val finishProgress: Float = 1f
)
