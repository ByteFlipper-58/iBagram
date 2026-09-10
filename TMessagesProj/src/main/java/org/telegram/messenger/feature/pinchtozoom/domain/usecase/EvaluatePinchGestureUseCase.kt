package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchGestureDecision
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchTouchPoint
import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class EvaluatePinchGestureUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(
        startDistance: Float,
        startCenterX: Float,
        startCenterY: Float,
        point1: PinchTouchPoint,
        point2: PinchTouchPoint,
        isInOverlay: Boolean
    ): PinchGestureDecision = repository.evaluatePinchGesture(
        startDistance = startDistance,
        startCenterX = startCenterX,
        startCenterY = startCenterY,
        point1 = point1,
        point2 = point2,
        isInOverlay = isInOverlay
    )
}
