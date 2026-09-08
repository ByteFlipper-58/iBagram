package org.telegram.messenger.feature.datastorage.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.DownloadController
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.StatsController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.data.mapper.DataStorageMapper
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.model.StorageUsageModel
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository
import java.io.File

class LegacyDataStorageRepository(
    private val currentAccount: Int,
    private val statsControllerProvider: () -> StatsController = { StatsController.getInstance(currentAccount) },
    private val downloadControllerProvider: () -> DownloadController = { DownloadController.getInstance(currentAccount) },
    private val messagesStorageProvider: () -> MessagesStorage = { MessagesStorage.getInstance(currentAccount) },
    private val messagesControllerProvider: () -> MessagesController = { MessagesController.getInstance(currentAccount) }
) : DataStorageRepository {

    private val storageUsageFlow = MutableStateFlow(StorageUsageModel())
    private val mobileUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.MOBILE))
    private val wifiUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.WIFI))
    private val roamingUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.ROAMING))

    private val mobilePresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.MOBILE))
    private val wifiPresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.WIFI))
    private val roamingPresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.ROAMING))

    private val keepMediaFlow = MutableStateFlow(KeepMediaSettingsModel())

    override fun observeNetworkUsage(type: NetworkUsageType): Flow<NetworkUsageModel> {
        return when (type) {
            NetworkUsageType.MOBILE -> mobileUsageFlow.asStateFlow()
            NetworkUsageType.WIFI -> wifiUsageFlow.asStateFlow()
            NetworkUsageType.ROAMING -> roamingUsageFlow.asStateFlow()
        }
    }

    override fun observeStorageUsage(): Flow<StorageUsageModel> = storageUsageFlow.asStateFlow()

    override fun observeAutoDownloadPreset(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel> {
        return when (type) {
            AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.asStateFlow()
            AutoDownloadNetworkType.WIFI -> wifiPresetFlow.asStateFlow()
            AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.asStateFlow()
        }
    }

    override fun observeKeepMediaSettings(): Flow<KeepMediaSettingsModel> = keepMediaFlow.asStateFlow()

    override suspend fun getNetworkUsage(type: NetworkUsageType): Result<NetworkUsageModel> = withContext(Dispatchers.Main) {
        try {
            val stats = statsControllerProvider()
            val model = DataStorageMapper.mapNetworkUsage(stats, type)
            when (type) {
                NetworkUsageType.MOBILE -> mobileUsageFlow.value = model
                NetworkUsageType.WIFI -> wifiUsageFlow.value = model
                NetworkUsageType.ROAMING -> roamingUsageFlow.value = model
            }
            Result.Success(model)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun resetNetworkUsage(type: NetworkUsageType): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val stats = statsControllerProvider()
            val netType = when (type) {
                NetworkUsageType.MOBILE -> StatsController.TYPE_MOBILE
                NetworkUsageType.WIFI -> StatsController.TYPE_WIFI
                NetworkUsageType.ROAMING -> StatsController.TYPE_ROAMING
            }
            stats.resetStats(netType)
            val updated = DataStorageMapper.mapNetworkUsage(stats, type)
            when (type) {
                NetworkUsageType.MOBILE -> mobileUsageFlow.value = updated
                NetworkUsageType.WIFI -> wifiUsageFlow.value = updated
                NetworkUsageType.ROAMING -> roamingUsageFlow.value = updated
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun getStorageUsage(): Result<StorageUsageModel> = withContext(Dispatchers.IO) {
        try {
            val dbSize = try {
                messagesStorageProvider().databaseSize
            } catch (e: Throwable) {
                0L
            }

            val photos = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE))
            val videos = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO))
            val documents = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT))
            val audio = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_AUDIO))
            val stories = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_STORIES))
            val cache = getDirSize(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE))

            val model = StorageUsageModel(
                photosBytes = photos,
                videosBytes = videos,
                documentsBytes = documents,
                audioBytes = audio,
                storiesBytes = stories,
                cacheTempBytes = cache,
                databaseBytes = dbSize
            )
            storageUsageFlow.value = model
            Result.Success(model)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun clearCache(
        clearPhotos: Boolean,
        clearVideos: Boolean,
        clearDocuments: Boolean,
        clearMusic: Boolean,
        clearAudio: Boolean,
        clearStickers: Boolean,
        clearStories: Boolean,
        clearOther: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (clearPhotos) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE))
            if (clearVideos) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO))
            if (clearDocuments) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT))
            if (clearAudio) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_AUDIO))
            if (clearStories) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_STORIES))
            if (clearOther) deleteDirContents(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE))

            getStorageUsage()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun clearDatabase(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            messagesStorageProvider().clearLocalDatabase()
            getStorageUsage()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun getAutoDownloadPreset(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel> = withContext(Dispatchers.Main) {
        try {
            val dc = downloadControllerProvider()
            val preset = when (type) {
                AutoDownloadNetworkType.MOBILE -> dc.mobilePreset
                AutoDownloadNetworkType.WIFI -> dc.wifiPreset
                AutoDownloadNetworkType.ROAMING -> dc.roamingPreset
            }
            val model = DataStorageMapper.mapAutoDownloadPreset(preset, type)
            when (type) {
                AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.value = model
                AutoDownloadNetworkType.WIFI -> wifiPresetFlow.value = model
                AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.value = model
            }
            Result.Success(model)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun updateAutoDownloadPreset(preset: AutoDownloadPresetModel): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val dc = downloadControllerProvider()
            val target = when (preset.networkType) {
                AutoDownloadNetworkType.MOBILE -> dc.mobilePreset
                AutoDownloadNetworkType.WIFI -> dc.wifiPreset
                AutoDownloadNetworkType.ROAMING -> dc.roamingPreset
            }
            DataStorageMapper.applyPresetModel(preset, target)
            val preferences = MessagesController.getMainSettings(currentAccount)
            val key = when (preset.networkType) {
                AutoDownloadNetworkType.MOBILE -> "mobilePreset"
                AutoDownloadNetworkType.WIFI -> "wifiPreset"
                AutoDownloadNetworkType.ROAMING -> "roamingPreset"
            }
            preferences.edit().putString(key, target.toString()).apply()

            when (preset.networkType) {
                AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.value = preset
                AutoDownloadNetworkType.WIFI -> wifiPresetFlow.value = preset
                AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.value = preset
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun getKeepMediaSettings(): Result<KeepMediaSettingsModel> = withContext(Dispatchers.Main) {
        try {
            val cacheByChats = messagesControllerProvider().cacheByChatsController
            val model = DataStorageMapper.mapKeepMediaSettings(cacheByChats)
            keepMediaFlow.value = model
            Result.Success(model)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun updateKeepMedia(chatType: Int, keepMediaDuration: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val cacheByChats = messagesControllerProvider().cacheByChatsController
            cacheByChats.setKeepMedia(chatType, keepMediaDuration)
            val updated = DataStorageMapper.mapKeepMediaSettings(cacheByChats)
            keepMediaFlow.value = updated
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Operation failed", e))
        }
    }

    override suspend fun refreshStorageUsage(): Result<Unit> = withContext(Dispatchers.IO) {
        getStorageUsage()
        Result.Success(Unit)
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirSize(f) else f.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File?) {
        if (dir == null || !dir.exists()) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                deleteDirContents(f)
            }
            f.delete()
        }
    }
}
