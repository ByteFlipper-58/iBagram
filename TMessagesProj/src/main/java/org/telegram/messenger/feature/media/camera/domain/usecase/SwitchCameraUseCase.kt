package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class SwitchCameraUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke() = repository.switchCamera()
}
