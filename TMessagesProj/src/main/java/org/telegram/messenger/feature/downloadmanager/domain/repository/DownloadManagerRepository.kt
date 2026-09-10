package org.telegram.messenger.feature.downloadmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadManagerState
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadPresetModel
import org.telegram.messenger.feature.downloadmanager.domain.model.PeerTypePreset

interface DownloadManagerRepository {
    fun observeState(): Flow<DownloadManagerState>
    fun getState(): DownloadManagerState
    fun observeDownloadingFiles(): Flow<List<DownloadItemModel>>
    fun getDownloadingFiles(): List<DownloadItemModel>
    fun observeRecentFiles(): Flow<List<DownloadItemModel>>
    fun getRecentFiles(): List<DownloadItemModel>
    fun enqueueDownload(item: DownloadItemModel): Boolean
    fun pauseDownload(id: String): Boolean
    fun resumeDownload(id: String): Boolean
    fun cancelDownload(id: String): Boolean
    fun retryDownload(id: String): Boolean
    fun deleteRecentDownload(id: String): Boolean
    fun clearRecentDownloads()
    fun markDownloadsAsViewed()
    fun updateDownloadProgress(id: String, downloadedBytes: Long, totalBytes: Long)
    fun completeDownload(id: String, path: String)
    fun failDownload(id: String, canceled: Boolean)
    fun setNetworkType(network: AutoDownloadNetwork)
    fun shouldAutoDownload(mediaType: AutoDownloadMediaType, peerType: PeerTypePreset, sizeBytes: Long): Boolean
    fun getPreset(network: AutoDownloadNetwork): DownloadPresetModel
    fun updatePreset(network: AutoDownloadNetwork, preset: DownloadPresetModel)
}
