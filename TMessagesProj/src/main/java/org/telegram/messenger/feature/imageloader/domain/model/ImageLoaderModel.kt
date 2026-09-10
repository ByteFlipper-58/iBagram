package org.telegram.messenger.feature.imageloader.domain.model

enum class ImageCacheTier {
    DEFAULT,
    SMALL,
    WALLPAPER,
    LOTTIE
}

enum class ImageLoadingStatus {
    IDLE,
    QUEUED,
    LOADING,
    LOADED,
    FAILED,
    CANCELLED
}

enum class FrameExtractType {
    NONE,
    FIRST_FRAME,
    LAST_FRAME,
    LAST_REACT_FRAME,
    PREVIEW_FRAME
}

data class ImageFilterSpec(
    val width: Int = 0,
    val height: Int = 0,
    val isWallpaper: Boolean = false,
    val isIgnoreCacheForSmall: Boolean = false,
    val isBlur: Boolean = false,
    val blurRadius: Int = 0,
    val isAutoplay: Boolean = false,
    val isAutoplayNonLoop: Boolean = false,
    val isRound: Boolean = false,
    val ignoreOrientation: Boolean = false,
    val checkExif: Boolean = false,
    val frameExtractType: FrameExtractType = FrameExtractType.NONE
)

data class ImageDownscaleSpec(
    val srcWidth: Int,
    val srcHeight: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val sampleSize: Int,
    val scaledWidth: Int,
    val scaledHeight: Int
)

data class ImageRequestModel(
    val key: String,
    val url: String? = null,
    val filter: String? = null,
    val filterSpec: ImageFilterSpec = ImageFilterSpec(),
    val targetTier: ImageCacheTier = ImageCacheTier.DEFAULT,
    val priority: Int = 0,
    val canForce8888: Boolean = false,
    val status: ImageLoadingStatus = ImageLoadingStatus.IDLE,
    val progress: Float = 0f,
    val loadedBytes: Long = 0L,
    val totalBytes: Long = 0L
) {
    val isFinished: Boolean
        get() = status == ImageLoadingStatus.LOADED ||
                status == ImageLoadingStatus.FAILED ||
                status == ImageLoadingStatus.CANCELLED
}

data class TierCacheStats(
    val tier: ImageCacheTier,
    val itemCount: Int = 0,
    val sizeBytes: Long = 0L,
    val maxSizeBytes: Long = 0L,
    val hitCount: Int = 0,
    val missCount: Int = 0
)

data class ImageCacheStatsModel(
    val totalItems: Int = 0,
    val totalSizeBytes: Long = 0L,
    val maxSizeBytes: Long = 0L,
    val totalHits: Int = 0,
    val totalMisses: Int = 0,
    val tiers: Map<ImageCacheTier, TierCacheStats> = emptyMap()
) {
    val hitRate: Float
        get() {
            val total = totalHits + totalMisses
            return if (total > 0) totalHits.toFloat() / total.toFloat() else 0f
        }
}

data class ImageLoaderState(
    val requests: List<ImageRequestModel> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val cacheStats: ImageCacheStatsModel = ImageCacheStatsModel(),
    val memoryPressureLevel: Int = 0
)
