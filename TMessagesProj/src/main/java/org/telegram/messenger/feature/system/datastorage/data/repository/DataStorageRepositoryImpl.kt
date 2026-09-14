package org.telegram.messenger.feature.system.datastorage.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.data.datasource.DataStorageLocalDataSource
import org.telegram.messenger.feature.system.datastorage.data.datasource.DataStorageRemoteDataSource
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.model.StorageUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository

/**
 * Implementation of [DataStorageRepository] coordinating [DataStorageLocalDataSource]
 * and [DataStorageRemoteDataSource].
 */
class DataStorageRepositoryImpl(
    private val localDataSource: DataStorageLocalDataSource,
    private val remoteDataSource: DataStorageRemoteDataSource
) : DataStorageRepository {

    override fun observeNetworkUsage(type: NetworkUsageType): Flow<NetworkUsageModel> {
        return localDataSource.observeNetworkUsage(type)
    }

    override fun observeStorageUsage(): Flow<StorageUsageModel> {
        return localDataSource.observeStorageUsage()
    }

    override fun observeAutoDownloadPreset(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel> {
        return localDataSource.observeAutoDownloadPreset(type)
    }

    override fun observeKeepMediaSettings(): Flow<KeepMediaSettingsModel> {
        return localDataSource.observeKeepMediaSettings()
    }

    override suspend fun getNetworkUsage(type: NetworkUsageType): Result<NetworkUsageModel> {
        return localDataSource.getNetworkUsage(type)
    }

    override suspend fun resetNetworkUsage(type: NetworkUsageType): Result<Unit> {
        return localDataSource.resetNetworkUsage(type)
    }

    override suspend fun getStorageUsage(): Result<StorageUsageModel> {
        return localDataSource.getStorageUsage()
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
    ): Result<Unit> {
        return localDataSource.clearCache(
            clearPhotos = clearPhotos,
            clearVideos = clearVideos,
            clearDocuments = clearDocuments,
            clearMusic = clearMusic,
            clearAudio = clearAudio,
            clearStickers = clearStickers,
            clearStories = clearStories,
            clearOther = clearOther
        )
    }

    override suspend fun clearDatabase(): Result<Unit> {
        return localDataSource.clearDatabase()
    }

    override suspend fun getAutoDownloadPreset(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel> {
        return localDataSource.getAutoDownloadPreset(type)
    }

    override suspend fun updateAutoDownloadPreset(preset: AutoDownloadPresetModel): Result<Unit> {
        return localDataSource.updateAutoDownloadPreset(preset)
    }

    override suspend fun getKeepMediaSettings(): Result<KeepMediaSettingsModel> {
        return localDataSource.getKeepMediaSettings()
    }

    override suspend fun updateKeepMedia(chatType: Int, keepMediaDuration: Int): Result<Unit> {
        return localDataSource.updateKeepMedia(chatType, keepMediaDuration)
    }

    override suspend fun refreshStorageUsage(): Result<Unit> {
        return localDataSource.refreshStorageUsage()
    }
}
