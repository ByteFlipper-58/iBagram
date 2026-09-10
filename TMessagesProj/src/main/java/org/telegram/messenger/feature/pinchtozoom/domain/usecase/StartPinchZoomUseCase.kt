package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class StartPinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(scale: Float = 1f) {
        repository.startZoom(scale)
    }
}
