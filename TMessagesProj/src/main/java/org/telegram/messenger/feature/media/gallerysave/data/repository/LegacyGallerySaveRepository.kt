package org.telegram.messenger.feature.media.gallerysave.data.repository

import android.util.LongSparseArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.SaveToGallerySettingsHelper
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.media.gallerysave.data.mapper.GallerySaveMapper
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository adapter bridging SaveToGallerySettingsHelper, UserConfig exceptions,
 * and pure domain models.
 */
class LegacyGallerySaveRepository(
    private val account: Int = 0
) : GallerySaveRepository {

    private val inMemorySettings = ConcurrentHashMap<GallerySavePeerType, GallerySaveTargetSettingsModel>()
    private val inMemoryExceptions = ConcurrentHashMap<GallerySavePeerType, MutableList<GallerySaveDialogExceptionModel>>()

    private val _configFlow = MutableStateFlow(readCurrentConfig())

    override fun observeConfig(): Flow<GallerySaveConfigModel> = _configFlow.asStateFlow()

    override fun getConfig(): GallerySaveConfigModel = _configFlow.value

    override fun getSettings(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel {
        return _configFlow.value.getSettings(peerType)
    }

    override fun updateSettings(settings: GallerySaveTargetSettingsModel) {
        val flag = GallerySaveMapper.toLegacyFlag(settings.peerType)
        if (isLegacyAvailable()) {
            try {
                val legacySettings = SaveToGallerySettingsHelper.getSettings(flag)
                if (legacySettings != null) {
                    legacySettings.savePhoto = settings.savePhoto
                    legacySettings.saveVideo = settings.saveVideo
                    legacySettings.limitVideo = settings.limitVideoBytes
                    SaveToGallerySettingsHelper.saveSettings(flag)
                }
            } catch (_: Throwable) {
                inMemorySettings[settings.peerType] = settings
            }
        } else {
            inMemorySettings[settings.peerType] = settings
        }
        updateState()
    }

    override fun togglePeerType(peerType: GallerySavePeerType) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val legacySettings = SaveToGallerySettingsHelper.getSettings(flag)
                legacySettings?.toggle()
                SaveToGallerySettingsHelper.saveSettings(flag)
            } catch (_: Throwable) {
                val current = inMemorySettings[peerType] ?: GallerySaveTargetSettingsModel(peerType)
                val toggled = current.copy(savePhoto = !current.savePhoto, saveVideo = !current.saveVideo)
                inMemorySettings[peerType] = toggled
            }
        } else {
            val current = inMemorySettings[peerType] ?: GallerySaveTargetSettingsModel(peerType)
            val toggled = current.copy(savePhoto = !current.savePhoto, saveVideo = !current.saveVideo)
            inMemorySettings[peerType] = toggled
        }
        updateState()
    }

    override fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long) {
        val current = getSettings(peerType)
        val clamped = limitBytes.coerceIn(0L, GallerySaveTargetSettingsModel.MAX_VIDEO_LIMIT_BYTES)
        updateSettings(current.copy(limitVideoBytes = clamped))
    }

    override fun getExceptions(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag)
                if (sparse != null) {
                    val list = mutableListOf<GallerySaveDialogExceptionModel>()
                    for (i in 0 until sparse.size()) {
                        val ex = sparse.valueAt(i)
                        if (ex != null) {
                            list.add(GallerySaveMapper.toDomainException(ex))
                        }
                    }
                    return list
                }
            } catch (_: Throwable) {
                // fall through to inMemory
            }
        }
        return inMemoryExceptions[peerType] ?: emptyList()
    }

    override fun setException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag) ?: LongSparseArray()
                sparse.put(exception.dialogId, GallerySaveMapper.toLegacyException(exception))
                userConfig?.updateSaveGalleryExceptions(flag, sparse)
            } catch (_: Throwable) {
                val list = inMemoryExceptions.computeIfAbsent(peerType) { mutableListOf() }
                list.removeAll { it.dialogId == exception.dialogId }
                list.add(exception)
            }
        } else {
            val list = inMemoryExceptions.computeIfAbsent(peerType) { mutableListOf() }
            list.removeAll { it.dialogId == exception.dialogId }
            list.add(exception)
        }
        updateState()
    }

    override fun removeException(peerType: GallerySavePeerType, dialogId: Long) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag)
                if (sparse != null) {
                    sparse.remove(dialogId)
                    userConfig.updateSaveGalleryExceptions(flag, sparse)
                }
            } catch (_: Throwable) {
                inMemoryExceptions[peerType]?.removeAll { it.dialogId == dialogId }
            }
        } else {
            inMemoryExceptions[peerType]?.removeAll { it.dialogId == dialogId }
        }
        updateState()
    }

    override fun removeAllExceptions(peerType: GallerySavePeerType) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val emptySparse = LongSparseArray<SaveToGallerySettingsHelper.DialogException>()
                userConfig?.updateSaveGalleryExceptions(flag, emptySparse)
            } catch (_: Throwable) {
                inMemoryExceptions[peerType]?.clear()
            }
        } else {
            inMemoryExceptions[peerType]?.clear()
        }
        updateState()
    }

    private fun isLegacyAvailable(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun readCurrentConfig(): GallerySaveConfigModel {
        val userModel = if (isLegacyAvailable()) {
            try {
                GallerySaveMapper.toDomainSettings(GallerySavePeerType.PEER, SaveToGallerySettingsHelper.user)
            } catch (_: Throwable) {
                inMemorySettings[GallerySavePeerType.PEER] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.PEER)
            }
        } else {
            inMemorySettings[GallerySavePeerType.PEER] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.PEER)
        }

        val groupModel = if (isLegacyAvailable()) {
            try {
                GallerySaveMapper.toDomainSettings(GallerySavePeerType.GROUP, SaveToGallerySettingsHelper.groups)
            } catch (_: Throwable) {
                inMemorySettings[GallerySavePeerType.GROUP] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.GROUP)
            }
        } else {
            inMemorySettings[GallerySavePeerType.GROUP] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.GROUP)
        }

        val channelModel = if (isLegacyAvailable()) {
            try {
                GallerySaveMapper.toDomainSettings(GallerySavePeerType.CHANNEL, SaveToGallerySettingsHelper.channels)
            } catch (_: Throwable) {
                inMemorySettings[GallerySavePeerType.CHANNEL] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.CHANNEL)
            }
        } else {
            inMemorySettings[GallerySavePeerType.CHANNEL] ?: GallerySaveTargetSettingsModel(GallerySavePeerType.CHANNEL)
        }

        val exceptionsMap = mutableMapOf<GallerySavePeerType, List<GallerySaveDialogExceptionModel>>()
        for (peerType in GallerySavePeerType.entries) {
            exceptionsMap[peerType] = getExceptions(peerType)
        }

        return GallerySaveConfigModel(
            userSettings = userModel,
            groupSettings = groupModel,
            channelSettings = channelModel,
            exceptions = exceptionsMap
        )
    }

    private fun updateState() {
        _configFlow.value = readCurrentConfig()
    }
}
