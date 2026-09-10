package org.telegram.messenger.feature.system.datastorage.domain.model

data class AutoDownloadPresetModel(
    val networkType: AutoDownloadNetworkType,
    val isEnabled: Boolean = true,
    val downloadPhotos: Boolean = true,
    val downloadVideos: Boolean = true,
    val downloadDocuments: Boolean = true,
    val maxVideoSize: Long = 10 * 1024 * 1024L,
    val maxDocumentSize: Long = 1024 * 1024L,
    val preloadVideo: Boolean = true,
    val preloadMusic: Boolean = true,
    val preloadStories: Boolean = true,
    val lessCallData: Boolean = false
)
