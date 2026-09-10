package org.telegram.messenger.feature.media.camera.domain.model

/**
 * Domain model describing a hardware camera device.
 */
data class CameraDeviceModel(
    val id: Int,
    val facing: CameraFacing,
    val previewResolutions: List<CameraResolutionModel> = emptyList(),
    val pictureResolutions: List<CameraResolutionModel> = emptyList()
) {
    val isFront: Boolean
        get() = facing == CameraFacing.FRONT

    val maxPictureResolution: CameraResolutionModel?
        get() = pictureResolutions.maxByOrNull { it.area }

    val maxPreviewResolution: CameraResolutionModel?
        get() = previewResolutions.maxByOrNull { it.area }
}
