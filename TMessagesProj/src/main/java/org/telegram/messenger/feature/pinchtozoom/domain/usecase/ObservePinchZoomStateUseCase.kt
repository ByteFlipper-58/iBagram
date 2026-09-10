package org.telegram.messenger.feature.pinchtozoom.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.pinchtozoom.domain.model.PinchZoomState
import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository

class ObservePinchZoomStateUseCase(
    private val repository: PinchToZoomRepository
) {
    operator fun invoke(): StateFlow<PinchZoomState> = repository.observeState()
}
