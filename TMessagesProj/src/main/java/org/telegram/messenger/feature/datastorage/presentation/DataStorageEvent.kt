package org.telegram.messenger.feature.datastorage.presentation

import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadPresetModel
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType

sealed class DataStorageEvent {
    object RefreshStorage : DataStorageEvent()
    data class ResetNetwork(val type: NetworkUsageType) : DataStorageEvent()
    data class ClearCache(
        val clearPhotos: Boolean = true,
        val clearVideos: Boolean = true,
        val clearDocuments: Boolean = true,
        val clearMusic: Boolean = true,
        val clearAudio: Boolean = true,
        val clearStickers: Boolean = true,
        val clearStories: Boolean = true,
        val clearOther: Boolean = true
    ) : DataStorageEvent()
    object ClearDatabase : DataStorageEvent()
    data class UpdatePreset(val preset: AutoDownloadPresetModel) : DataStorageEvent()
    data class UpdateKeepMedia(val chatType: Int, val duration: Int) : DataStorageEvent()
}
