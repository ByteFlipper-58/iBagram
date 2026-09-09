package org.telegram.messenger.feature.camera.domain.usecase

import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class SwitchCameraUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke() = repository.switchCamera()
}
