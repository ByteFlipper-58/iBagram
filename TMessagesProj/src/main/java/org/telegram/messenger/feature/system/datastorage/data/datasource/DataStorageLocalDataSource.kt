package org.telegram.messenger.feature.system.datastorage.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.DownloadController
import org.telegram.messenger.FileLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.StatsController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.model.StorageUsageModel
import java.io.File

/**
 * Local data source managing device cache sizes, storage calculations,
 * network usage stats, and auto-download presets.
 */
class DataStorageLocalDataSource(
    private val currentAccount: Int = 0,
    var testMode: Boolean = false
) {

    private val storageUsageFlow = MutableStateFlow(StorageUsageModel())
    private val mobileUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.MOBILE))
    private val wifiUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.WIFI))
    private val roamingUsageFlow = MutableStateFlow(NetworkUsageModel(NetworkUsageType.ROAMING))

    private val mobilePresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.MOBILE))
    private val wifiPresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.WIFI))
    private val roamingPresetFlow = MutableStateFlow(AutoDownloadPresetModel(AutoDownloadNetworkType.ROAMING))

    private val keepMediaFlow = MutableStateFlow(KeepMediaSettingsModel())

    fun observeNetworkUsage(type: NetworkUsageType): Flow<NetworkUsageModel> {
        return when (type) {
            NetworkUsageType.MOBILE -> mobileUsageFlow.asStateFlow()
            NetworkUsageType.WIFI -> wifiUsageFlow.asStateFlow()
            NetworkUsageType.ROAMING -> roamingUsageFlow.asStateFlow()
        }
    }

    fun observeStorageUsage(): Flow<StorageUsageModel> = storageUsageFlow.asStateFlow()

    fun observeAutoDownloadPreset(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel> {
        return when (type) {
            AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.asStateFlow()
            AutoDownloadNetworkType.WIFI -> wifiPresetFlow.asStateFlow()
            AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.asStateFlow()
        }
    }

    fun observeKeepMediaSettings(): Flow<KeepMediaSettingsModel> = keepMediaFlow.asStateFlow()

    suspend fun getNetworkUsage(type: NetworkUsageType): Result<NetworkUsageModel> {
        return when (type) {
            NetworkUsageType.MOBILE -> Result.Success(mobileUsageFlow.value)
            NetworkUsageType.WIFI -> Result.Success(wifiUsageFlow.value)
            NetworkUsageType.ROAMING -> Result.Success(roamingUsageFlow.value)
        }
    }

    suspend fun resetNetworkUsage(type: NetworkUsageType): Result<Unit> {
        val resetModel = NetworkUsageModel(networkType = type)
        when (type) {
            NetworkUsageType.MOBILE -> mobileUsageFlow.value = resetModel
            NetworkUsageType.WIFI -> wifiUsageFlow.value = resetModel
            NetworkUsageType.ROAMING -> roamingUsageFlow.value = resetModel
        }
        if (!testMode) {
            runCatching {
                val stats = StatsController.getInstance(currentAccount)
                val networkType = when (type) {
                    NetworkUsageType.MOBILE -> StatsController.TYPE_MOBILE
                    NetworkUsageType.WIFI -> StatsController.TYPE_WIFI
                    NetworkUsageType.ROAMING -> StatsController.TYPE_ROAMING
                }
                stats.resetStats(networkType)
            }
        }
        return Result.Success(Unit)
    }

    suspend fun getStorageUsage(): Result<StorageUsageModel> {
        return Result.Success(storageUsageFlow.value)
    }

    suspend fun clearCache(
        clearPhotos: Boolean = true,
        clearVideos: Boolean = true,
        clearDocuments: Boolean = true,
        clearMusic: Boolean = true,
        clearAudio: Boolean = true,
        clearStickers: Boolean = true,
        clearStories: Boolean = true,
        clearOther: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (testMode) {
            val current = storageUsageFlow.value
            storageUsageFlow.value = current.copy(
                photosBytes = if (clearPhotos) 0L else current.photosBytes,
                videosBytes = if (clearVideos) 0L else current.videosBytes,
                documentsBytes = if (clearDocuments) 0L else current.documentsBytes,
                musicBytes = if (clearMusic) 0L else current.musicBytes,
                audioBytes = if (clearAudio) 0L else current.audioBytes,
                stickersBytes = if (clearStickers) 0L else current.stickersBytes,
                storiesBytes = if (clearStories) 0L else current.storiesBytes,
                otherBytes = if (clearOther) 0L else current.otherBytes
            )
            return@withContext Result.Success(Unit)
        }

        try {
            val directoryTypes = mutableListOf<Int>()
            if (clearPhotos) directoryTypes.add(FileLoader.MEDIA_DIR_IMAGE)
            if (clearVideos) directoryTypes.add(FileLoader.MEDIA_DIR_VIDEO)
            if (clearDocuments) directoryTypes.add(FileLoader.MEDIA_DIR_DOCUMENT)
            if (clearAudio) directoryTypes.add(FileLoader.MEDIA_DIR_AUDIO)
            if (clearStories) directoryTypes.add(FileLoader.MEDIA_DIR_STORIES)
            if (clearOther) directoryTypes.add(FileLoader.MEDIA_DIR_CACHE)

            for (dirType in directoryTypes) {
                runCatching {
                    val dir = FileLoader.checkDirectory(dirType)
                    if (dir != null && dir.exists()) {
                        dir.listFiles()?.forEach { file ->
                            if (file.isFile) file.delete()
                        }
                    }
                }
            }
            refreshStorageUsage()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to clear cache: ${e.message}", e))
        }
    }

    suspend fun clearDatabase(): Result<Unit> = withContext(Dispatchers.IO) {
        if (testMode) {
            storageUsageFlow.value = storageUsageFlow.value.copy(databaseBytes = 0L)
            return@withContext Result.Success(Unit)
        }

        try {
            val storage = MessagesStorage.getInstance(currentAccount)
            storage.clearLocalDatabase()
            refreshStorageUsage()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Database("Failed to clear database: ${e.message}", e))
        }
    }

    suspend fun getAutoDownloadPreset(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel> {
        val preset = when (type) {
            AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.value
            AutoDownloadNetworkType.WIFI -> wifiPresetFlow.value
            AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.value
        }
        return Result.Success(preset)
    }

    suspend fun updateAutoDownloadPreset(preset: AutoDownloadPresetModel): Result<Unit> {
        when (preset.networkType) {
            AutoDownloadNetworkType.MOBILE -> mobilePresetFlow.value = preset
            AutoDownloadNetworkType.WIFI -> wifiPresetFlow.value = preset
            AutoDownloadNetworkType.ROAMING -> roamingPresetFlow.value = preset
        }
        if (!testMode) {
            try {
                val downloadController = DownloadController.getInstance(currentAccount)
                val targetConfig = when (preset.networkType) {
                    AutoDownloadNetworkType.MOBILE -> downloadController.mobilePreset
                    AutoDownloadNetworkType.WIFI -> downloadController.wifiPreset
                    AutoDownloadNetworkType.ROAMING -> downloadController.roamingPreset
                }
                org.telegram.messenger.feature.system.datastorage.data.mapper.DataStorageMapper.applyPresetModel(preset, targetConfig)
                val typeIndex = when (preset.networkType) {
                    AutoDownloadNetworkType.MOBILE -> 0
                    AutoDownloadNetworkType.WIFI -> 1
                    AutoDownloadNetworkType.ROAMING -> 2
                }
                downloadController.savePresetToServer(typeIndex)
            } catch (_: Throwable) {
            }
        }
        return Result.Success(Unit)
    }

    suspend fun getKeepMediaSettings(): Result<KeepMediaSettingsModel> {
        return Result.Success(keepMediaFlow.value)
    }

    suspend fun updateKeepMedia(chatType: Int, keepMediaDuration: Int): Result<Unit> {
        val current = keepMediaFlow.value
        val updated = when (chatType) {
            0 -> current.copy(keepMediaUser = keepMediaDuration)
            1 -> current.copy(keepMediaGroup = keepMediaDuration)
            2 -> current.copy(keepMediaChannel = keepMediaDuration)
            else -> current.copy(keepMediaStories = keepMediaDuration)
        }
        keepMediaFlow.value = updated
        if (!testMode) {
            try {
                val cacheController = CacheByChatsController(currentAccount)
                cacheController.setKeepMedia(chatType, keepMediaDuration)
            } catch (_: Throwable) {
            }
        }
        return Result.Success(Unit)
    }

    suspend fun refreshStorageUsage(): Result<Unit> = withContext(Dispatchers.IO) {
        if (testMode) {
            return@withContext Result.Success(Unit)
        }

        try {
            fun getDirSize(dirType: Int): Long {
                val dir = FileLoader.checkDirectory(dirType) ?: return 0L
                var size = 0L
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) size += file.length()
                    }
                }
                return size
            }

            val photos = getDirSize(FileLoader.MEDIA_DIR_IMAGE)
            val videos = getDirSize(FileLoader.MEDIA_DIR_VIDEO)
            val documents = getDirSize(FileLoader.MEDIA_DIR_DOCUMENT)
            val audio = getDirSize(FileLoader.MEDIA_DIR_AUDIO)
            val stories = getDirSize(FileLoader.MEDIA_DIR_STORIES)
            val cache = getDirSize(FileLoader.MEDIA_DIR_CACHE)

            val dbSize = try {
                MessagesStorage.getInstance(currentAccount).databaseSize
            } catch (_: Throwable) {
                0L
            }

            storageUsageFlow.value = StorageUsageModel(
                photosBytes = photos,
                videosBytes = videos,
                documentsBytes = documents,
                audioBytes = audio,
                storiesBytes = stories,
                otherBytes = cache,
                databaseBytes = dbSize
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic("Failed to calculate storage size: ${e.message}", e))
        }
    }

    fun updateStorageUsageState(model: StorageUsageModel) {
        storageUsageFlow.value = model
    }
}
