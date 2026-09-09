package org.telegram.messenger.feature.camera.presentation

import org.telegram.messenger.feature.camera.domain.model.CameraDeviceModel
import org.telegram.messenger.feature.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.camera.domain.model.CameraRecordingState
import org.telegram.messenger.feature.camera.domain.model.CameraResolutionModel
import org.telegram.messenger.feature.camera.domain.model.CameraStateModel

/**
 * Immutable UI State for camera preview, configuration, and capture workflows.
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val state: CameraStateModel = CameraStateModel(),
    val optimalPreviewResolution: CameraResolutionModel? = null,
    val optimalPictureResolution: CameraResolutionModel? = null,
    val infoMessage: String? = null
) {
    val isInitialized: Boolean
        get() = state.isInitialized

    val availableCameras: List<CameraDeviceModel>
        get() = state.availableCameras

    val selectedCamera: CameraDeviceModel?
        get() = state.selectedCamera

    val flashMode: CameraFlashMode
        get() = state.flashMode

    val isMirrorFrontCamera: Boolean
        get() = state.isMirrorFrontCamera

    val recordingState: CameraRecordingState
        get() = state.recordingState

    val isRecording: Boolean
        get() = state.isRecording

    val canSwitchCamera: Boolean
        get() = state.hasMultipleCameras && !isRecording
}
