package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class SelectCameraUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(cameraId: Int) = repository.selectCamera(cameraId)
}
