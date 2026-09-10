package org.telegram.messenger.feature.media.imageloader.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoaderState
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel

interface ImageLoaderRepository {
    fun observeState(): Flow<ImageLoaderState>
    fun getState(): ImageLoaderState
    fun observeCacheStats(): Flow<ImageCacheStatsModel>
    fun getCacheStats(): ImageCacheStatsModel
    fun hasInCache(key: String, tier: ImageCacheTier = ImageCacheTier.DEFAULT): Boolean
    fun putCacheEntry(key: String, sizeBytes: Long, tier: ImageCacheTier = ImageCacheTier.DEFAULT)
    fun removeCacheEntry(key: String, tier: ImageCacheTier = ImageCacheTier.DEFAULT): Boolean
    fun clearCache(tier: ImageCacheTier? = null)
    fun trimMemory(level: Int)
    fun enqueueRequest(request: ImageRequestModel): Boolean
    fun cancelRequest(key: String): Boolean
    fun updateRequestStatus(
        key: String,
        status: ImageLoadingStatus,
        progress: Float = 0f,
        loadedBytes: Long = 0L,
        totalBytes: Long = 0L
    )
    fun recordCacheHit(tier: ImageCacheTier)
    fun recordCacheMiss(tier: ImageCacheTier)
}
