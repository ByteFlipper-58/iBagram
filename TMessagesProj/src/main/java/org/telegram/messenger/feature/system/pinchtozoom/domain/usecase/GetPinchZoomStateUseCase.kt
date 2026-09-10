package org.telegram.messenger.feature.system.pinchtozoom.domain.usecase

import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchZoomState
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository

class GetPinchZoomStateUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(): PinchZoomState = repository.getState()
}
