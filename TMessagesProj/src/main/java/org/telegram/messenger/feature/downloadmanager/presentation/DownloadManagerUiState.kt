package org.telegram.messenger.feature.downloadmanager.presentation

import org.telegram.messenger.feature.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadManagerStats
import org.telegram.messenger.feature.downloadmanager.domain.model.DownloadPresetModel

data class DownloadManagerUiState(
    val downloadingFiles: List<DownloadItemModel> = emptyList(),
    val recentDownloadingFiles: List<DownloadItemModel> = emptyList(),
    val unviewedDownloads: List<DownloadItemModel> = emptyList(),
    val currentNetwork: AutoDownloadNetwork = AutoDownloadNetwork.WIFI,
    val activePreset: DownloadPresetModel = DownloadPresetModel(),
    val stats: DownloadManagerStats = DownloadManagerStats(),
    val isPausedAll: Boolean = false,
    val errorMessage: String? = null
)
