package org.telegram.messenger.feature.media.downloadmanager.presentation

import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadPresetModel

sealed class DownloadManagerEvent {
    data class EnqueueDownload(val item: DownloadItemModel) : DownloadManagerEvent()
    data class PauseDownload(val id: String) : DownloadManagerEvent()
    data class ResumeDownload(val id: String) : DownloadManagerEvent()
    data class CancelDownload(val id: String) : DownloadManagerEvent()
    data class RetryDownload(val id: String) : DownloadManagerEvent()
    data class DeleteRecentDownload(val id: String) : DownloadManagerEvent()
    object ClearRecentDownloads : DownloadManagerEvent()
    object MarkDownloadsAsViewed : DownloadManagerEvent()
    data class SetNetworkType(val network: AutoDownloadNetwork) : DownloadManagerEvent()
    data class UpdatePreset(val network: AutoDownloadNetwork, val preset: DownloadPresetModel) : DownloadManagerEvent()
}
