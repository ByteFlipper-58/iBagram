package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchZoomState
import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class GetPinchZoomStateUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(): PinchZoomState = repository.getState()
}
