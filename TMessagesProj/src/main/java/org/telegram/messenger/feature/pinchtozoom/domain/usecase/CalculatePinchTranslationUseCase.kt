package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class CalculatePinchTranslationUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(
        startCenterX: Float,
        startCenterY: Float,
        currentCenterX: Float,
        currentCenterY: Float,
        scale: Float
    ): Pair<Float, Float> = repository.calculateTranslation(
        startCenterX = startCenterX,
        startCenterY = startCenterY,
        currentCenterX = currentCenterX,
        currentCenterY = currentCenterY,
        scale = scale
    )
}
