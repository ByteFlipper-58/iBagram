package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class ResetPinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
