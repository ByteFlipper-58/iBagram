package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class ToggleMirrorFrontCameraUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(mirror: Boolean) = repository.toggleMirrorFrontCamera(mirror)
}
