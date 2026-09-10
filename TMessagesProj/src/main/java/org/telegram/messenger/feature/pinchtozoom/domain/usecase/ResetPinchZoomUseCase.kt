package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class ResetPinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
