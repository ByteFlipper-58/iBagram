package org.telegram.messenger.feature.media.imageloader.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoaderState
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel
import org.telegram.messenger.feature.media.imageloader.domain.model.TierCacheStats
import java.util.concurrent.ConcurrentHashMap

class ImageLoaderLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val requests = ConcurrentHashMap<String, ImageRequestModel>()
    private val cacheMap = ConcurrentHashMap<ImageCacheTier, ConcurrentHashMap<String, Long>>()
    private val hitsMap = ConcurrentHashMap<ImageCacheTier, Int>()
    private val missesMap = ConcurrentHashMap<ImageCacheTier, Int>()

    init {
        ImageCacheTier.values().forEach { tier ->
            cacheMap[tier] = ConcurrentHashMap()
            hitsMap[tier] = 0
            missesMap[tier] = 0
        }
    }

    private val _cacheStatsFlow = MutableStateFlow(computeCacheStats())
    val cacheStatsFlow: StateFlow<ImageCacheStatsModel> = _cacheStatsFlow.asStateFlow()

    private val _stateFlow = MutableStateFlow(ImageLoaderState())
    val stateFlow: StateFlow<ImageLoaderState> = _stateFlow.asStateFlow()

    private fun computeCacheStats(): ImageCacheStatsModel {
        var totalItems = 0
        var totalSize = 0L
        var totalHits = 0
        var totalMisses = 0
        val tiersMap = mutableMapOf<ImageCacheTier, TierCacheStats>()

        ImageCacheTier.values().forEach { tier ->
            val tierItems = cacheMap[tier]?.size ?: 0
            val tierSize = cacheMap[tier]?.values?.sum() ?: 0L
            val tierHits = hitsMap[tier] ?: 0
            val tierMisses = missesMap[tier] ?: 0

            totalItems += tierItems
            totalSize += tierSize
            totalHits += tierHits
            totalMisses += tierMisses

            tiersMap[tier] = TierCacheStats(
                tier = tier,
                itemCount = tierItems,
                sizeBytes = tierSize,
                maxSizeBytes = 100L * 1024L * 1024L,
                hitCount = tierHits,
                missCount = tierMisses
            )
        }

        return ImageCacheStatsModel(
            totalItems = totalItems,
            totalSizeBytes = totalSize,
            maxSizeBytes = 400L * 1024L * 1024L,
            totalHits = totalHits,
            totalMisses = totalMisses,
            tiers = tiersMap
        )
    }

    private fun updateState(memoryPressure: Int = _stateFlow.value.memoryPressureLevel) {
        val reqList = requests.values.toList()
        val active = reqList.count { it.status == ImageLoadingStatus.LOADING || it.status == ImageLoadingStatus.QUEUED }
        val completed = reqList.count { it.status == ImageLoadingStatus.LOADED }
        val failed = reqList.count { it.status == ImageLoadingStatus.FAILED }
        val stats = computeCacheStats()
        _cacheStatsFlow.value = stats
        _stateFlow.value = ImageLoaderState(
            requests = reqList,
            activeCount = active,
            completedCount = completed,
            failedCount = failed,
            cacheStats = stats,
            memoryPressureLevel = memoryPressure
        )
    }

    fun hasInCache(key: String, tier: ImageCacheTier): Boolean {
        return cacheMap[tier]?.containsKey(key) ?: false
    }

    fun putCacheEntry(key: String, sizeBytes: Long, tier: ImageCacheTier) {
        cacheMap[tier]?.put(key, sizeBytes)
        updateState()
    }

    fun removeCacheEntry(key: String, tier: ImageCacheTier): Boolean {
        val removed = cacheMap[tier]?.remove(key) != null
        if (removed) {
            updateState()
        }
        return removed
    }

    fun clearCache(tier: ImageCacheTier? = null) {
        if (tier != null) {
            cacheMap[tier]?.clear()
        } else {
            cacheMap.values.forEach { it.clear() }
        }
        updateState()
    }

    fun trimMemory(level: Int) {
        if (level >= 80) { // TRIM_MEMORY_COMPLETE
            clearCache()
        } else if (level >= 40) { // TRIM_MEMORY_BACKGROUND
            cacheMap[ImageCacheTier.DEFAULT]?.clear()
            cacheMap[ImageCacheTier.SMALL]?.clear()
        }
        updateState(memoryPressure = level)
    }

    fun enqueueRequest(request: ImageRequestModel): Boolean {
        requests[request.key] = request.copy(status = ImageLoadingStatus.QUEUED)
        updateState()
        return true
    }

    fun cancelRequest(key: String): Boolean {
        val existing = requests[key] ?: return false
        requests[key] = existing.copy(status = ImageLoadingStatus.CANCELLED)
        updateState()
        return true
    }

    fun updateRequestStatus(
        key: String,
        status: ImageLoadingStatus,
        progress: Float = 0f,
        loadedBytes: Long = 0L,
        totalBytes: Long = 0L
    ) {
        val existing = requests[key]
        if (existing != null) {
            requests[key] = existing.copy(
                status = status,
                progress = progress,
                loadedBytes = loadedBytes,
                totalBytes = totalBytes
            )
            updateState()
        }
    }

    fun recordCacheHit(tier: ImageCacheTier) {
        hitsMap[tier] = (hitsMap[tier] ?: 0) + 1
        updateState()
    }

    fun recordCacheMiss(tier: ImageCacheTier) {
        missesMap[tier] = (missesMap[tier] ?: 0) + 1
        updateState()
    }
}
