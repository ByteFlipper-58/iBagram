package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchBoundsResult
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class CalculatePinchImageBoundsUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(dimensions: PinchImageDimensions, scale: Float): PinchBoundsResult =
        repository.calculateImageBounds(dimensions, scale)
}
