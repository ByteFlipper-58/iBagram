package org.telegram.messenger.feature.camera.domain.usecase

import org.telegram.messenger.feature.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class SetCameraFlashModeUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(mode: CameraFlashMode) = repository.setFlashMode(mode)
}
