package org.telegram.messenger.feature.camera.domain.usecase

import org.telegram.messenger.feature.camera.domain.model.CameraStateModel
import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class GetCameraStateUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(): CameraStateModel = repository.getCameraState()
}
