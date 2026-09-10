package org.telegram.messenger.feature.downloadmanager.domain.model

enum class AutoDownloadMediaType {
    PHOTO,
    VIDEO,
    DOCUMENT,
    AUDIO
}

enum class AutoDownloadNetwork {
    CELLULAR,
    WIFI,
    ROAMING
}

enum class PeerTypePreset {
    CONTACTS,
    PRIVATE_CHATS,
    GROUPS,
    CHANNELS
}

enum class DownloadItemStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadPresetModel(
    val mask: Map<PeerTypePreset, Set<AutoDownloadMediaType>> = emptyMap(),
    val maxSizes: Map<AutoDownloadMediaType, Long> = emptyMap(),
    val preloadVideo: Boolean = true,
    val preloadMusic: Boolean = true,
    val preloadStories: Boolean = true,
    val lessCallData: Boolean = false,
    val maxVideoBitrate: Int = 1000,
    val enabled: Boolean = true
) {
    fun isMediaAllowed(peerType: PeerTypePreset, mediaType: AutoDownloadMediaType, sizeBytes: Long): Boolean {
        if (!enabled) return false
        val allowedTypes = mask[peerType] ?: emptySet()
        if (!allowedTypes.contains(mediaType)) return false
        val maxSize = maxSizes[mediaType] ?: Long.MAX_VALUE
        return sizeBytes <= maxSize
    }
}

data class DownloadItemModel(
    val id: String,
    val fileName: String,
    val path: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long,
    val downloadedBytes: Long = 0L,
    val status: DownloadItemStatus = DownloadItemStatus.QUEUED,
    val progress: Float = 0f,
    val isViewed: Boolean = false,
    val timestamp: Long = 0L,
    val dialogId: Long = 0L
) {
    val isFinished: Boolean
        get() = status == DownloadItemStatus.COMPLETED ||
                status == DownloadItemStatus.FAILED ||
                status == DownloadItemStatus.CANCELLED
}

data class DownloadManagerStats(
    val totalActiveDownloads: Int = 0,
    val totalCompletedDownloads: Int = 0,
    val unviewedCount: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val currentDownloadSpeedBytesPerSec: Long = 0L
)

data class DownloadManagerState(
    val downloadingFiles: List<DownloadItemModel> = emptyList(),
    val recentDownloadingFiles: List<DownloadItemModel> = emptyList(),
    val unviewedDownloads: List<DownloadItemModel> = emptyList(),
    val currentNetwork: AutoDownloadNetwork = AutoDownloadNetwork.WIFI,
    val activePreset: DownloadPresetModel = DownloadPresetModel(),
    val stats: DownloadManagerStats = DownloadManagerStats()
)
