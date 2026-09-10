package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class StartPinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(scale: Float = 1f) {
        repository.startZoom(scale)
    }
}
