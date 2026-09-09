package org.telegram.messenger.feature.camera.domain.usecase

import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class SelectCameraUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(cameraId: Int) = repository.selectCamera(cameraId)
}
