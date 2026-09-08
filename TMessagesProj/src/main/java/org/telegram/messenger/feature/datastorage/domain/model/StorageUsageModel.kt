package org.telegram.messenger.feature.datastorage.domain.model

data class StorageUsageModel(
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val musicBytes: Long = 0L,
    val stickersBytes: Long = 0L,
    val storiesBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val cacheTempBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val totalDeviceBytes: Long = 0L,
    val totalDeviceFreeBytes: Long = 0L
) {
    val totalCacheBytes: Long
        get() = photosBytes + videosBytes + documentsBytes + audioBytes +
                musicBytes + stickersBytes + storiesBytes + otherBytes + cacheTempBytes

    val totalTelegramBytes: Long
        get() = totalCacheBytes + databaseBytes
}
