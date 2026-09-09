package org.telegram.messenger.feature.gallerysave.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.gallerysave.domain.model.GallerySaveTargetSettingsModel

/**
 * Domain repository contract for Telegram auto-save to gallery preferences and exceptions.
 */
interface GallerySaveRepository {
    fun observeConfig(): Flow<GallerySaveConfigModel>
    fun getConfig(): GallerySaveConfigModel
    fun getSettings(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel
    fun updateSettings(settings: GallerySaveTargetSettingsModel)
    fun togglePeerType(peerType: GallerySavePeerType)
    fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long)
    fun getExceptions(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel>
    fun setException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel)
    fun removeException(peerType: GallerySavePeerType, dialogId: Long)
    fun removeAllExceptions(peerType: GallerySavePeerType)
}
