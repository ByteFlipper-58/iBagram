package org.telegram.messenger.feature.media.imageloader.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.imageloader.data.datasource.ImageLoaderLocalDataSource
import org.telegram.messenger.feature.media.imageloader.data.datasource.ImageLoaderRemoteDataSource
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoaderState
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel
import org.telegram.messenger.feature.media.imageloader.domain.repository.ImageLoaderRepository

class ImageLoaderRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: ImageLoaderLocalDataSource,
    private val remoteDataSource: ImageLoaderRemoteDataSource
) : ImageLoaderRepository {

    override fun observeState(): Flow<ImageLoaderState> {
        return localDataSource.stateFlow
    }

    override fun getState(): ImageLoaderState {
        return localDataSource.stateFlow.value
    }

    override fun observeCacheStats(): Flow<ImageCacheStatsModel> {
        return localDataSource.cacheStatsFlow
    }

    override fun getCacheStats(): ImageCacheStatsModel {
        return localDataSource.cacheStatsFlow.value
    }

    override fun hasInCache(key: String, tier: ImageCacheTier): Boolean {
        return localDataSource.hasInCache(key, tier)
    }

    override fun putCacheEntry(key: String, sizeBytes: Long, tier: ImageCacheTier) {
        localDataSource.putCacheEntry(key, sizeBytes, tier)
    }

    override fun removeCacheEntry(key: String, tier: ImageCacheTier): Boolean {
        return localDataSource.removeCacheEntry(key, tier)
    }

    override fun clearCache(tier: ImageCacheTier?) {
        localDataSource.clearCache(tier)
    }

    override fun trimMemory(level: Int) {
        localDataSource.trimMemory(level)
    }

    override fun enqueueRequest(request: ImageRequestModel): Boolean {
        return localDataSource.enqueueRequest(request)
    }

    override fun cancelRequest(key: String): Boolean {
        return localDataSource.cancelRequest(key)
    }

    override fun updateRequestStatus(
        key: String,
        status: ImageLoadingStatus,
        progress: Float,
        loadedBytes: Long,
        totalBytes: Long
    ) {
        localDataSource.updateRequestStatus(key, status, progress, loadedBytes, totalBytes)
    }

    override fun recordCacheHit(tier: ImageCacheTier) {
        localDataSource.recordCacheHit(tier)
    }

    override fun recordCacheMiss(tier: ImageCacheTier) {
        localDataSource.recordCacheMiss(tier)
    }
}
