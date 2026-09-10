package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTransform
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class CalculatePinchTransformUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(
        scale: Float,
        finishProgress: Float,
        parentOffsetX: Float,
        parentOffsetY: Float,
        pinchCenterX: Float,
        pinchCenterY: Float,
        translationX: Float,
        translationY: Float
    ): PinchTransform = repository.calculateTransform(
        scale = scale,
        finishProgress = finishProgress,
        parentOffsetX = parentOffsetX,
        parentOffsetY = parentOffsetY,
        pinchCenterX = pinchCenterX,
        pinchCenterY = pinchCenterY,
        translationX = translationX,
        translationY = translationY
    )
}
