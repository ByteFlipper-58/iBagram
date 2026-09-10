package org.telegram.messenger.feature.autodeletemedia.domain.model

/**
 * Domain models for background media auto-deletion and cache eviction.
 */

data class CacheLimitConfig(
    val maxCacheSizeGb: Int = Int.MAX_VALUE,
    val maxCacheSizeBytes: Long = calculateMaxCacheSizeBytes(maxCacheSizeGb)
) {
    companion object {
        fun calculateMaxCacheSizeBytes(maxCacheSizeGb: Int): Long {
            return when {
                maxCacheSizeGb == Int.MAX_VALUE -> Long.MAX_VALUE
                maxCacheSizeGb == 1 -> 1024L * 1024L * 300L // 300 MB special threshold in Telegram
                maxCacheSizeGb > 1 -> maxCacheSizeGb * 1024L * 1024L * 1000L
                else -> Long.MAX_VALUE
            }
        }
    }
}

data class MediaScanFileModel(
    val path: String,
    val sizeBytes: Long,
    val lastUsageTimeSec: Long,
    val isStory: Boolean = false,
    val isLocked: Boolean = false,
    val dialogType: Int = -1,
    val keepMediaDays: Int = 30,
    val isKeepForever: Boolean = false
)

data class AutoDeleteRunResult(
    val filesScanned: Int = 0,
    val autoDeletedFiles: Int = 0,
    val autoDeletedBytes: Long = 0L,
    val deletedBySizeLimitFiles: Int = 0,
    val deletedBySizeLimitBytes: Long = 0L,
    val skippedFiles: Int = 0,
    val durationMs: Long = 0L
)

data class AutoDeleteTaskState(
    val lastCheckTimeSec: Long = 0L,
    val isRunning: Boolean = false,
    val lockedFilesCount: Int = 0,
    val lastResult: AutoDeleteRunResult? = null
)
