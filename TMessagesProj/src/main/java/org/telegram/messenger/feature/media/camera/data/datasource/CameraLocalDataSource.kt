package org.telegram.messenger.feature.media.camera.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.camera.domain.model.CameraDeviceModel
import org.telegram.messenger.feature.media.camera.domain.model.CameraFacing
import org.telegram.messenger.feature.media.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.media.camera.domain.model.CameraRecordingState
import org.telegram.messenger.feature.media.camera.domain.model.CameraResolutionModel
import org.telegram.messenger.feature.media.camera.domain.model.CameraStateModel

/**
 * Local data source managing active camera state, available devices,
 * flash mode, front-facing mirror settings, and recording metrics.
 */
open class CameraLocalDataSource(
    protected val currentAccount: Int
) {
    private val lock = Any()
    private val _stateFlow = MutableStateFlow(CameraStateModel())

    open fun observeCameraState(): Flow<CameraStateModel> = _stateFlow.asStateFlow()

    open fun getCameraState(): CameraStateModel = synchronized(lock) { _stateFlow.value }

    open fun updateState(update: (CameraStateModel) -> CameraStateModel) {
        synchronized(lock) {
            _stateFlow.value = update(_stateFlow.value)
        }
    }

    open fun setupHeadlessFallback() {
        synchronized(lock) {
            val backCamera = CameraDeviceModel(
                id = 0,
                facing = CameraFacing.BACK,
                previewResolutions = listOf(
                    CameraResolutionModel(1920, 1080),
                    CameraResolutionModel(1280, 720),
                    CameraResolutionModel(640, 480)
                ),
                pictureResolutions = listOf(
                    CameraResolutionModel(4000, 3000),
                    CameraResolutionModel(1920, 1080)
                )
            )
            val frontCamera = CameraDeviceModel(
                id = 1,
                facing = CameraFacing.FRONT,
                previewResolutions = listOf(
                    CameraResolutionModel(1280, 720),
                    CameraResolutionModel(640, 480)
                ),
                pictureResolutions = listOf(
                    CameraResolutionModel(1920, 1080)
                )
            )
            _stateFlow.value = _stateFlow.value.copy(
                isInitialized = true,
                isLoading = false,
                availableCameras = listOf(backCamera, frontCamera),
                selectedCameraId = 0
            )
        }
    }

    open fun selectCamera(cameraId: Int) {
        synchronized(lock) {
            val exists = _stateFlow.value.availableCameras.any { it.id == cameraId }
            if (exists) {
                _stateFlow.value = _stateFlow.value.copy(selectedCameraId = cameraId)
            }
        }
    }

    open fun switchCamera() {
        synchronized(lock) {
            val currentList = _stateFlow.value.availableCameras
            if (currentList.size <= 1) return
            val currentId = _stateFlow.value.selectedCameraId ?: return
            val currentIndex = currentList.indexOfFirst { it.id == currentId }
            val nextIndex = (currentIndex + 1) % currentList.size
            _stateFlow.value = _stateFlow.value.copy(selectedCameraId = currentList[nextIndex].id)
        }
    }

    open fun setFlashMode(mode: CameraFlashMode) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(flashMode = mode)
        }
    }

    open fun toggleMirrorFrontCamera(mirror: Boolean) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(isMirrorFrontCamera = mirror)
        }
    }

    open fun notifyRecordingStarted(filePath: String) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(
                recordingState = CameraRecordingState.RECORDING,
                lastRecordedFilePath = filePath,
                errorMessage = null
            )
        }
    }

    open fun notifyRecordingFinished(filePath: String, durationMs: Long) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(
                recordingState = CameraRecordingState.FINISHED,
                lastRecordedFilePath = filePath,
                lastRecordedDurationMs = durationMs,
                errorMessage = null
            )
        }
    }

    open fun notifyRecordingFailed(errorMessage: String) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(
                recordingState = CameraRecordingState.FAILED,
                errorMessage = errorMessage
            )
        }
    }

    open fun chooseOptimalResolution(
        resolutions: List<CameraResolutionModel>,
        targetWidth: Int,
        targetHeight: Int,
        targetAspectWidth: Int,
        targetAspectHeight: Int,
        notBigger: Boolean
    ): CameraResolutionModel? {
        if (resolutions.isEmpty()) return null

        val bigEnoughWithAspectRatio = mutableListOf<CameraResolutionModel>()
        val bigEnough = mutableListOf<CameraResolutionModel>()

        for (res in resolutions) {
            if (notBigger && (res.height > targetHeight || res.width > targetWidth)) {
                continue
            }
            val matchesAspect = res.hasSameAspectRatio(targetAspectWidth, targetAspectHeight)
            if (matchesAspect && res.width >= targetWidth && res.height >= targetHeight) {
                bigEnoughWithAspectRatio.add(res)
            } else if (res.area <= targetWidth.toLong() * targetHeight.toLong() * 4) {
                bigEnough.add(res)
            }
        }

        return when {
            bigEnoughWithAspectRatio.isNotEmpty() -> bigEnoughWithAspectRatio.minByOrNull { it.area }
            bigEnough.isNotEmpty() -> bigEnough.minByOrNull { it.area }
            else -> resolutions.maxByOrNull { it.area }
        }
    }
}
