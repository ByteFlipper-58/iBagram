package org.telegram.messenger.feature.media.camera.presentation

import org.telegram.messenger.feature.media.camera.domain.model.CameraFlashMode

/**
 * MVI intents for camera interactions.
 */
sealed class CameraEvent {
    object InitCameras : CameraEvent()
    data class SelectCamera(val cameraId: Int) : CameraEvent()
    object SwitchCamera : CameraEvent()
    data class SetFlashMode(val mode: CameraFlashMode) : CameraEvent()
    data class ToggleMirrorFront(val mirror: Boolean) : CameraEvent()
    data class ComputeOptimalResolutions(
        val targetWidth: Int,
        val targetHeight: Int,
        val aspectWidth: Int,
        val aspectHeight: Int
    ) : CameraEvent()
    data class RecordStarted(val filePath: String) : CameraEvent()
    data class RecordFinished(val filePath: String, val durationMs: Long) : CameraEvent()
    data class RecordFailed(val errorMessage: String) : CameraEvent()
    object DismissInfo : CameraEvent()
}
