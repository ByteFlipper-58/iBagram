package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class InitCamerasUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(): Boolean = repository.initCameras()
}
