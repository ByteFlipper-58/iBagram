package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class CalculatePinchScaleUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(currentDistance: Float, startDistance: Float): Float =
        repository.calculateScale(currentDistance, startDistance)
}
