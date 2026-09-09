package org.telegram.messenger.feature.camera.domain.usecase

import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class InitCamerasUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(): Boolean = repository.initCameras()
}
