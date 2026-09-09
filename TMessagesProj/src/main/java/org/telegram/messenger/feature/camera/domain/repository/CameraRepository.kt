package org.telegram.messenger.feature.camera.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.camera.domain.model.CameraFlashMode
import org.telegram.messenger.feature.camera.domain.model.CameraResolutionModel
import org.telegram.messenger.feature.camera.domain.model.CameraStateModel

/**
 * Clean domain contract for hardware camera operations, resolution selection, and recording state.
 */
interface CameraRepository {
    fun observeCameraState(): Flow<CameraStateModel>
    fun getCameraState(): CameraStateModel
    fun initCameras(): Boolean
    fun selectCamera(cameraId: Int)
    fun switchCamera()
    fun setFlashMode(mode: CameraFlashMode)
    fun toggleMirrorFrontCamera(mirror: Boolean)
    fun chooseOptimalResolution(
        resolutions: List<CameraResolutionModel>,
        targetWidth: Int,
        targetHeight: Int,
        targetAspectWidth: Int,
        targetAspectHeight: Int,
        notBigger: Boolean = false
    ): CameraResolutionModel?
    fun notifyRecordingStarted(filePath: String)
    fun notifyRecordingFinished(filePath: String, durationMs: Long)
    fun notifyRecordingFailed(errorMessage: String)
}
