package org.telegram.messenger.feature.camera.data.mapper

import org.telegram.messenger.camera.CameraInfo
import org.telegram.messenger.camera.Size
import org.telegram.messenger.feature.camera.domain.model.CameraDeviceModel
import org.telegram.messenger.feature.camera.domain.model.CameraFacing
import org.telegram.messenger.feature.camera.domain.model.CameraResolutionModel

/**
 * Pure mapping functions translating legacy CameraInfo and Size into clean domain models.
 */
object CameraMapper {

    fun toDomainResolution(size: Size): CameraResolutionModel {
        return CameraResolutionModel(
            width = size.mWidth,
            height = size.mHeight
        )
    }

    fun toDomainDevice(cameraInfo: CameraInfo): CameraDeviceModel {
        val facing = if (cameraInfo.isFrontface) CameraFacing.FRONT else CameraFacing.BACK
        val previewResolutions = cameraInfo.previewSizes?.map { toDomainResolution(it) } ?: emptyList()
        val pictureResolutions = cameraInfo.pictureSizes?.map { toDomainResolution(it) } ?: emptyList()

        return CameraDeviceModel(
            id = cameraInfo.cameraId,
            facing = facing,
            previewResolutions = previewResolutions,
            pictureResolutions = pictureResolutions
        )
    }

    fun toDomainDevices(cameraInfos: List<CameraInfo>?): List<CameraDeviceModel> {
        if (cameraInfos == null) return emptyList()
        return cameraInfos.map { toDomainDevice(it) }
    }
}
