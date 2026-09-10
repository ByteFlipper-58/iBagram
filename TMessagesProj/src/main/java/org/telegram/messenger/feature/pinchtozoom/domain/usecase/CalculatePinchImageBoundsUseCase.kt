package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class CalculatePinchImageBoundsUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(dimensions: PinchImageDimensions, scale: Float): PinchBoundsResult =
        repository.calculateImageBounds(dimensions, scale)
}
