package org.telegram.messenger.feature.media.gallerysave.data.mapper

import org.telegram.messenger.SaveToGallerySettingsHelper
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel

/**
 * Pure mapper between legacy SaveToGallerySettingsHelper structures and pure domain models.
 */
object GallerySaveMapper {

    fun toDomainPeerType(flag: Int): GallerySavePeerType {
        return when (flag) {
            SharedConfig.SAVE_TO_GALLERY_FLAG_PEER -> GallerySavePeerType.PEER
            SharedConfig.SAVE_TO_GALLERY_FLAG_GROUP -> GallerySavePeerType.GROUP
            SharedConfig.SAVE_TO_GALLERY_FLAG_CHANNELS -> GallerySavePeerType.CHANNEL
            else -> GallerySavePeerType.PEER
        }
    }

    fun toLegacyFlag(peerType: GallerySavePeerType): Int {
        return when (peerType) {
            GallerySavePeerType.PEER -> SharedConfig.SAVE_TO_GALLERY_FLAG_PEER
            GallerySavePeerType.GROUP -> SharedConfig.SAVE_TO_GALLERY_FLAG_GROUP
            GallerySavePeerType.CHANNEL -> SharedConfig.SAVE_TO_GALLERY_FLAG_CHANNELS
        }
    }

    fun toDomainSettings(
        peerType: GallerySavePeerType,
        legacy: SaveToGallerySettingsHelper.SharedSettings?
    ): GallerySaveTargetSettingsModel {
        if (legacy == null) {
            return GallerySaveTargetSettingsModel(peerType = peerType)
        }
        return GallerySaveTargetSettingsModel(
            peerType = peerType,
            savePhoto = legacy.savePhoto,
            saveVideo = legacy.saveVideo,
            limitVideoBytes = legacy.limitVideo
        )
    }

    fun toDomainException(legacy: SaveToGallerySettingsHelper.DialogException): GallerySaveDialogExceptionModel {
        return GallerySaveDialogExceptionModel(
            dialogId = legacy.dialogId,
            savePhoto = legacy.savePhoto,
            saveVideo = legacy.saveVideo,
            limitVideoBytes = legacy.limitVideo
        )
    }

    fun toLegacyException(domain: GallerySaveDialogExceptionModel): SaveToGallerySettingsHelper.DialogException {
        val legacy = SaveToGallerySettingsHelper.DialogException()
        legacy.dialogId = domain.dialogId
        legacy.savePhoto = domain.savePhoto
        legacy.saveVideo = domain.saveVideo
        legacy.limitVideo = domain.limitVideoBytes
        return legacy
    }
}
