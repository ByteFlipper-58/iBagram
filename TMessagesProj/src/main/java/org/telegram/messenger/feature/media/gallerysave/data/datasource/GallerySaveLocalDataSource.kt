package org.telegram.messenger.feature.media.gallerysave.data.datasource

import android.util.LongSparseArray
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.SaveToGallerySettingsHelper
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.media.gallerysave.data.mapper.GallerySaveMapper
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveConfigModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel
import java.util.concurrent.ConcurrentHashMap

/**
 * Local data source managing SaveToGallery preferences in SharedPreferences and memory caches.
 */
open class GallerySaveLocalDataSource(
    private val account: Int = 0
) {

    private val inMemorySettings = ConcurrentHashMap<GallerySavePeerType, GallerySaveTargetSettingsModel>()
    private val inMemoryExceptions = ConcurrentHashMap<GallerySavePeerType, MutableList<GallerySaveDialogExceptionModel>>()

    protected open fun isLegacyAvailable(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null &&
                    SaveToGallerySettingsHelper.user != null &&
                    UserConfig.getInstance(account) != null
        } catch (_: Throwable) {
            false
        }
    }

    open fun readCurrentConfig(): GallerySaveConfigModel {
        val userSettings = readTargetSettings(GallerySavePeerType.PEER)
        val groupSettings = readTargetSettings(GallerySavePeerType.GROUP)
        val channelSettings = readTargetSettings(GallerySavePeerType.CHANNEL)

        val exceptionsMap = mutableMapOf<GallerySavePeerType, List<GallerySaveDialogExceptionModel>>()
        exceptionsMap[GallerySavePeerType.PEER] = readExceptions(GallerySavePeerType.PEER)
        exceptionsMap[GallerySavePeerType.GROUP] = readExceptions(GallerySavePeerType.GROUP)
        exceptionsMap[GallerySavePeerType.CHANNEL] = readExceptions(GallerySavePeerType.CHANNEL)

        return GallerySaveConfigModel(
            userSettings = userSettings,
            groupSettings = groupSettings,
            channelSettings = channelSettings,
            exceptions = exceptionsMap
        )
    }

    open fun readTargetSettings(peerType: GallerySavePeerType): GallerySaveTargetSettingsModel {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val legacySettings = SaveToGallerySettingsHelper.getSettings(flag)
                if (legacySettings is SaveToGallerySettingsHelper.SharedSettings) {
                    return GallerySaveMapper.toDomainSettings(peerType, legacySettings)
                }
            } catch (_: Throwable) {
                // Fallback to in-memory cache
            }
        }
        return inMemorySettings.getOrPut(peerType) {
            GallerySaveTargetSettingsModel(peerType = peerType)
        }
    }

    open fun saveTargetSettings(settings: GallerySaveTargetSettingsModel) {
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
    }

    open fun togglePeerType(peerType: GallerySavePeerType) {
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
    }

    open fun setVideoLimit(peerType: GallerySavePeerType, limitBytes: Long) {
        val current = readTargetSettings(peerType)
        val clamped = limitBytes.coerceIn(0L, GallerySaveTargetSettingsModel.MAX_VIDEO_LIMIT_BYTES)
        saveTargetSettings(current.copy(limitVideoBytes = clamped))
    }

    open fun readExceptions(peerType: GallerySavePeerType): List<GallerySaveDialogExceptionModel> {
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
                // Fallback to in-memory
            }
        }
        return inMemoryExceptions[peerType]?.toList() ?: emptyList()
    }

    open fun saveException(peerType: GallerySavePeerType, exception: GallerySaveDialogExceptionModel) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag)
                if (sparse != null) {
                    sparse.put(exception.dialogId, GallerySaveMapper.toLegacyException(exception))
                    userConfig.saveConfig(false)
                }
            } catch (_: Throwable) {
                val list = inMemoryExceptions.getOrPut(peerType) { mutableListOf() }
                list.removeAll { it.dialogId == exception.dialogId }
                list.add(exception)
            }
        } else {
            val list = inMemoryExceptions.getOrPut(peerType) { mutableListOf() }
            list.removeAll { it.dialogId == exception.dialogId }
            list.add(exception)
        }
    }

    open fun removeException(peerType: GallerySavePeerType, dialogId: Long) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag)
                if (sparse != null) {
                    sparse.remove(dialogId)
                    userConfig.saveConfig(false)
                }
            } catch (_: Throwable) {
                inMemoryExceptions[peerType]?.removeAll { it.dialogId == dialogId }
            }
        } else {
            inMemoryExceptions[peerType]?.removeAll { it.dialogId == dialogId }
        }
    }

    open fun removeAllExceptions(peerType: GallerySavePeerType) {
        val flag = GallerySaveMapper.toLegacyFlag(peerType)
        if (isLegacyAvailable()) {
            try {
                val userConfig = UserConfig.getInstance(account)
                val sparse = userConfig?.getSaveGalleryExceptions(flag)
                if (sparse != null) {
                    sparse.clear()
                    userConfig.saveConfig(false)
                }
            } catch (_: Throwable) {
                inMemoryExceptions[peerType]?.clear()
            }
        } else {
            inMemoryExceptions[peerType]?.clear()
        }
    }
}
