package org.telegram.messenger.feature.system.datastorage.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.system.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.system.datastorage.domain.model.KeepMediaSettingsModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageModel
import org.telegram.messenger.feature.system.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.system.datastorage.domain.model.StorageUsageModel

interface DataStorageRepository {
    fun observeNetworkUsage(type: NetworkUsageType): Flow<NetworkUsageModel>
    fun observeStorageUsage(): Flow<StorageUsageModel>
    fun observeAutoDownloadPreset(type: AutoDownloadNetworkType): Flow<AutoDownloadPresetModel>
    fun observeKeepMediaSettings(): Flow<KeepMediaSettingsModel>

    suspend fun getNetworkUsage(type: NetworkUsageType): Result<NetworkUsageModel>
    suspend fun resetNetworkUsage(type: NetworkUsageType): Result<Unit>
    suspend fun getStorageUsage(): Result<StorageUsageModel>
    suspend fun clearCache(
        clearPhotos: Boolean = true,
        clearVideos: Boolean = true,
        clearDocuments: Boolean = true,
        clearMusic: Boolean = true,
        clearAudio: Boolean = true,
        clearStickers: Boolean = true,
        clearStories: Boolean = true,
        clearOther: Boolean = true
    ): Result<Unit>
    suspend fun clearDatabase(): Result<Unit>
    suspend fun getAutoDownloadPreset(type: AutoDownloadNetworkType): Result<AutoDownloadPresetModel>
    suspend fun updateAutoDownloadPreset(preset: AutoDownloadPresetModel): Result<Unit>
    suspend fun getKeepMediaSettings(): Result<KeepMediaSettingsModel>
    suspend fun updateKeepMedia(chatType: Int, keepMediaDuration: Int): Result<Unit>
    suspend fun refreshStorageUsage(): Result<Unit>
}
