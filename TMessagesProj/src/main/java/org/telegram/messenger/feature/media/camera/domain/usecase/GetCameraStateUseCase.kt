package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.model.CameraStateModel
import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class GetCameraStateUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(): CameraStateModel = repository.getCameraState()
}
