package org.telegram.messenger.feature.media.camera.domain.model

/**
 * Composite domain state of camera availability, active selection, and recording status.
 */
data class CameraStateModel(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val availableCameras: List<CameraDeviceModel> = emptyList(),
    val selectedCameraId: Int? = null,
    val flashMode: CameraFlashMode = CameraFlashMode.OFF,
    val isMirrorFrontCamera: Boolean = false,
    val recordingState: CameraRecordingState = CameraRecordingState.IDLE,
    val lastRecordedFilePath: String? = null,
    val lastRecordedDurationMs: Long = 0L,
    val errorMessage: String? = null
) {
    val selectedCamera: CameraDeviceModel?
        get() = availableCameras.firstOrNull { it.id == selectedCameraId } ?: availableCameras.firstOrNull()

    val backCamera: CameraDeviceModel?
        get() = availableCameras.firstOrNull { it.facing == CameraFacing.BACK }

    val frontCamera: CameraDeviceModel?
        get() = availableCameras.firstOrNull { it.facing == CameraFacing.FRONT }

    val hasMultipleCameras: Boolean
        get() = availableCameras.size > 1

    val isRecording: Boolean
        get() = recordingState == CameraRecordingState.RECORDING
}
