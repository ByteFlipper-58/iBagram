package org.telegram.messenger.feature.media.camera.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.media.camera.domain.model.CameraStateModel
import org.telegram.messenger.feature.media.camera.domain.usecase.ChooseOptimalResolutionUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.GetCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.InitCamerasUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.NotifyCameraRecordingUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ObserveCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SelectCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SetCameraFlashModeUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SwitchCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ToggleMirrorFrontCameraUseCase

class CameraViewModel(
    private val observeCameraStateUseCase: ObserveCameraStateUseCase,
    private val getCameraStateUseCase: GetCameraStateUseCase,
    private val initCamerasUseCase: InitCamerasUseCase,
    private val selectCameraUseCase: SelectCameraUseCase,
    private val switchCameraUseCase: SwitchCameraUseCase,
    private val setCameraFlashModeUseCase: SetCameraFlashModeUseCase,
    private val toggleMirrorFrontCameraUseCase: ToggleMirrorFrontCameraUseCase,
    private val chooseOptimalResolutionUseCase: ChooseOptimalResolutionUseCase,
    private val notifyCameraRecordingUseCase: NotifyCameraRecordingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState(isLoading = true))
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        observeCameraStateUseCase()
            .onEach { cameraState ->
                updateFromState(cameraState)
            }
            .launchIn(viewModelScope)

        initCamerasUseCase()
    }

    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.InitCameras -> {
                initCamerasUseCase()
            }
            is CameraEvent.SelectCamera -> {
                selectCameraUseCase(event.cameraId)
            }
            is CameraEvent.SwitchCamera -> {
                switchCameraUseCase()
            }
            is CameraEvent.SetFlashMode -> {
                setCameraFlashModeUseCase(event.mode)
            }
            is CameraEvent.ToggleMirrorFront -> {
                toggleMirrorFrontCameraUseCase(event.mirror)
            }
            is CameraEvent.ComputeOptimalResolutions -> {
                val currentCam = _uiState.value.selectedCamera ?: return
                val optPreview = chooseOptimalResolutionUseCase(
                    resolutions = currentCam.previewResolutions,
                    targetWidth = event.targetWidth,
                    targetHeight = event.targetHeight,
                    targetAspectWidth = event.aspectWidth,
                    targetAspectHeight = event.aspectHeight
                )
                val optPicture = chooseOptimalResolutionUseCase(
                    resolutions = currentCam.pictureResolutions,
                    targetWidth = event.targetWidth,
                    targetHeight = event.targetHeight,
                    targetAspectWidth = event.aspectWidth,
                    targetAspectHeight = event.aspectHeight
                )
                _uiState.value = _uiState.value.copy(
                    optimalPreviewResolution = optPreview,
                    optimalPictureResolution = optPicture
                )
            }
            is CameraEvent.RecordStarted -> {
                notifyCameraRecordingUseCase.started(event.filePath)
            }
            is CameraEvent.RecordFinished -> {
                notifyCameraRecordingUseCase.finished(event.filePath, event.durationMs)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Recorded video saved (${event.durationMs} ms)"
                )
            }
            is CameraEvent.RecordFailed -> {
                notifyCameraRecordingUseCase.failed(event.errorMessage)
                _uiState.value = _uiState.value.copy(
                    infoMessage = "Recording failed: ${event.errorMessage}"
                )
            }
            is CameraEvent.DismissInfo -> {
                _uiState.value = _uiState.value.copy(infoMessage = null)
            }
        }
    }

    private fun updateFromState(cameraState: CameraStateModel) {
        _uiState.value = _uiState.value.copy(
            isLoading = cameraState.isLoading,
            state = cameraState
        )
    }
}
