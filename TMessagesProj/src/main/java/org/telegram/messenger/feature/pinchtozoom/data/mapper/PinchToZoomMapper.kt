package org.telegram.messenger.feature.pinchtozoom.data.mapper

import kotlin.math.hypot
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTransform

/**
 * Pure geometric and touch math engine for pinch-to-zoom gestures and transforms.
 */
object PinchToZoomMapper {

    /**
     * Threshold scale beyond which pinch-to-zoom overlay mode is triggered.
     * Matches Telegram's PinchToZoomHelper threshold (1.005f).
     */
    const val ZOOM_START_THRESHOLD = 1.005f

    fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return hypot(x2 - x1, y2 - y1)
    }

    fun calculateScale(currentDistance: Float, startDistance: Float): Float {
        if (startDistance <= 0f) return 1f
        return currentDistance / startDistance
    }

    fun calculateTranslation(
        startCenterX: Float,
        startCenterY: Float,
        currentCenterX: Float,
        currentCenterY: Float,
        scale: Float
    ): Pair<Float, Float> {
        val s = if (scale <= 0f) 1f else scale
        val moveDx = startCenterX - currentCenterX
        val moveDy = startCenterY - currentCenterY
        return Pair(-moveDx / s, -moveDy / s)
    }

    fun calculateTransform(
        scale: Float,
        finishProgress: Float,
        parentOffsetX: Float,
        parentOffsetY: Float,
        pinchCenterX: Float,
        pinchCenterY: Float,
        translationX: Float,
        translationY: Float
    ): PinchTransform {
        val s = scale * finishProgress + 1f * (1f - finishProgress)
        val pivotX = parentOffsetX + pinchCenterX
        val pivotY = parentOffsetY + pinchCenterY
        val transX = parentOffsetX + translationX * finishProgress
        val transY = parentOffsetY + translationY * finishProgress
        return PinchTransform(
            scale = s,
            pivotX = pivotX,
            pivotY = pivotY,
            translationX = transX,
            translationY = transY
        )
    }

    fun calculateImageBounds(dimensions: PinchImageDimensions, scale: Float): PinchBoundsResult {
        val p = when {
            scale < 1.0f -> 0f
            scale < 1.4f -> (scale - 1.0f) / 0.4f
            else -> 1.0f
        }
        val horizontalPadding = (dimensions.fullImageWidth - dimensions.imageWidth) / 2f
        val verticalPadding = (dimensions.fullImageHeight - dimensions.imageHeight) / 2f
        val x = dimensions.imageX - horizontalPadding * p
        val y = dimensions.imageY - verticalPadding * p
        val width = dimensions.imageWidth + horizontalPadding * p * 2f
        val height = dimensions.imageHeight + verticalPadding * p * 2f
        return PinchBoundsResult(
            x = x,
            y = y,
            width = width,
            height = height,
            progress = p
        )
    }

    fun evaluatePinchGesture(
        startDistance: Float,
        startCenterX: Float,
        startCenterY: Float,
        point1: PinchTouchPoint,
        point2: PinchTouchPoint,
        isInOverlay: Boolean
    ): PinchGestureDecision {
        val currentDistance = calculateDistance(point1.x, point1.y, point2.x, point2.y)
        val scale = calculateScale(currentDistance, startDistance)
        val currentCenterX = (point1.x + point2.x) / 2f
        val currentCenterY = (point1.y + point2.y) / 2f

        val shouldStart = scale > ZOOM_START_THRESHOLD && !isInOverlay
        val (transX, transY) = calculateTranslation(startCenterX, startCenterY, currentCenterX, currentCenterY, scale)

        return PinchGestureDecision(
            shouldStartZoom = shouldStart,
            scale = scale,
            translationX = transX,
            translationY = transY,
            centerX = currentCenterX,
            centerY = currentCenterY
        )
    }
}
