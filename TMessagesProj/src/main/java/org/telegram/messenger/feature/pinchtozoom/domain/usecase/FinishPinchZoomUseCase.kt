package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class FinishPinchZoomUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(progress: Float) {
        repository.finishZoom(progress)
    }
}
