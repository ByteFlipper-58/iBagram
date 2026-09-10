package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class UpdatePinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(scale: Float, translationX: Float, translationY: Float) {
        repository.updateZoom(scale, translationX, translationY)
    }
}
