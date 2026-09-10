package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class NotifyCameraRecordingUseCase(
    private val repository: CameraRepository
) {
    fun started(filePath: String) = repository.notifyRecordingStarted(filePath)
    fun finished(filePath: String, durationMs: Long) = repository.notifyRecordingFinished(filePath, durationMs)
    fun failed(errorMessage: String) = repository.notifyRecordingFailed(errorMessage)
}
