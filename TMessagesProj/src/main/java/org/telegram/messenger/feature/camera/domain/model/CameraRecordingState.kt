package org.telegram.messenger.feature.camera.domain.model

/**
 * Camera video recording lifecycle state.
 */
enum class CameraRecordingState {
    IDLE,
    RECORDING,
    FINISHED,
    FAILED
}
