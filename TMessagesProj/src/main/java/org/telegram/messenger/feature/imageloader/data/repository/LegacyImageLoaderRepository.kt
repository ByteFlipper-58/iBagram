package org.telegram.messenger.feature.imageloader.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.imageloader.domain.model.ImageCacheStatsModel
import org.telegram.messenger.feature.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.imageloader.domain.model.ImageLoaderState
import org.telegram.messenger.feature.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.imageloader.domain.model.ImageRequestModel
import org.telegram.messenger.feature.imageloader.domain.model.TierCacheStats
import org.telegram.messenger.feature.imageloader.domain.repository.ImageLoaderRepository
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

class LegacyImageLoaderRepository(
    private val currentAccount: Int = 0
) : ImageLoaderRepository {

    private val lock = Any()

    // Tier LRU caches (key -> size in bytes)
    private val defaultCache = Collections.synchronizedMap(LinkedHashMap<String, Long>(32, 0.75f, true))
    private val smallCache = Collections.synchronizedMap(LinkedHashMap<String, Long>(32, 0.75f, true))
    private val wallpaperCache = Collections.synchronizedMap(LinkedHashMap<String, Long>(16, 0.75f, true))
    private val lottieCache = Collections.synchronizedMap(LinkedHashMap<String, Long>(32, 0.75f, true))

    // Active & recent requests
    private val requestsMap = ConcurrentHashMap<String, ImageRequestModel>()

    // Cache metrics per tier
    private val hitsMap = ConcurrentHashMap<ImageCacheTier, Int>()
    private val missesMap = ConcurrentHashMap<ImageCacheTier, Int>()

    private val _state = MutableStateFlow(ImageLoaderState())
    override fun observeState(): Flow<ImageLoaderState> = _state.asStateFlow()
    override fun getState(): ImageLoaderState = _state.value

    private val _cacheStats = MutableStateFlow(ImageCacheStatsModel())
    override fun observeCacheStats(): Flow<ImageCacheStatsModel> = _cacheStats.asStateFlow()
    override fun getCacheStats(): ImageCacheStatsModel = _cacheStats.value

    init {
        updateStats()
    }

    private fun getMapForTier(tier: ImageCacheTier): MutableMap<String, Long> {
        return when (tier) {
            ImageCacheTier.DEFAULT -> defaultCache
            ImageCacheTier.SMALL -> smallCache
            ImageCacheTier.WALLPAPER -> wallpaperCache
            ImageCacheTier.LOTTIE -> lottieCache
        }
    }

    override fun hasInCache(key: String, tier: ImageCacheTier): Boolean {
        synchronized(lock) {
            return getMapForTier(tier).containsKey(key)
        }
    }

    override fun putCacheEntry(key: String, sizeBytes: Long, tier: ImageCacheTier) {
        synchronized(lock) {
            val map = getMapForTier(tier)
            map[key] = sizeBytes
            updateStats()
        }
    }

    override fun removeCacheEntry(key: String, tier: ImageCacheTier): Boolean {
        synchronized(lock) {
            val removed = getMapForTier(tier).remove(key) != null
            if (removed) {
                updateStats()
            }
            return removed
        }
    }

    override fun clearCache(tier: ImageCacheTier?) {
        synchronized(lock) {
            if (tier != null) {
                getMapForTier(tier).clear()
            } else {
                defaultCache.clear()
                smallCache.clear()
                wallpaperCache.clear()
                lottieCache.clear()
            }
            updateStats()
        }
    }

    override fun trimMemory(level: Int) {
        synchronized(lock) {
            when {
                // Critical pressure: trim all caches
                level >= 80 -> { // TRIM_MEMORY_COMPLETE
                    clearCache(null)
                }
                level >= 60 -> { // TRIM_MEMORY_MODERATE
                    trimMap(defaultCache, 0.5f)
                    trimMap(smallCache, 0.5f)
                    trimMap(wallpaperCache, 0.5f)
                    trimMap(lottieCache, 0.5f)
                }
                level >= 40 -> { // TRIM_MEMORY_BACKGROUND
                    trimMap(defaultCache, 0.25f)
                    trimMap(wallpaperCache, 0.25f)
                }
            }
            _state.value = _state.value.copy(memoryPressureLevel = level)
            updateStats()
        }
    }

    private fun trimMap(map: MutableMap<String, Long>, fractionToRemove: Float) {
        val removeCount = (map.size * fractionToRemove).toInt()
        val iterator = map.keys.iterator()
        var removed = 0
        while (iterator.hasNext() && removed < removeCount) {
            iterator.next()
            iterator.remove()
            removed++
        }
    }

    override fun enqueueRequest(request: ImageRequestModel): Boolean {
        synchronized(lock) {
            requestsMap[request.key] = request
            publishLoaderState()
            return true
        }
    }

    override fun cancelRequest(key: String): Boolean {
        synchronized(lock) {
            val existing = requestsMap[key] ?: return false
            if (!existing.isFinished) {
                requestsMap[key] = existing.copy(status = ImageLoadingStatus.CANCELLED)
                publishLoaderState()
                return true
            }
            return false
        }
    }

    override fun updateRequestStatus(
        key: String,
        status: ImageLoadingStatus,
        progress: Float,
        loadedBytes: Long,
        totalBytes: Long
    ) {
        synchronized(lock) {
            val existing = requestsMap[key] ?: return
            requestsMap[key] = existing.copy(
                status = status,
                progress = progress,
                loadedBytes = loadedBytes,
                totalBytes = totalBytes
            )
            publishLoaderState()
        }
    }

    override fun recordCacheHit(tier: ImageCacheTier) {
        hitsMap.merge(tier, 1) { a, b -> a + b }
        updateStats()
    }

    override fun recordCacheMiss(tier: ImageCacheTier) {
        missesMap.merge(tier, 1) { a, b -> a + b }
        updateStats()
    }

    private fun updateStats() {
        var totalItems = 0
        var totalBytes = 0L
        var totalHits = 0
        var totalMisses = 0

        val tierStatsMap = mutableMapOf<ImageCacheTier, TierCacheStats>()

        for (tier in ImageCacheTier.values()) {
            val map = getMapForTier(tier)
            val count = map.size
            var bytes = 0L
            for (size in map.values) {
                bytes += size
            }
            val hits = hitsMap[tier] ?: 0
            val misses = missesMap[tier] ?: 0

            val maxCap = when (tier) {
                ImageCacheTier.DEFAULT -> 64L * 1024L * 1024L
                ImageCacheTier.SMALL -> 16L * 1024L * 1024L
                ImageCacheTier.WALLPAPER -> 32L * 1024L * 1024L
                ImageCacheTier.LOTTIE -> 32L * 1024L * 1024L
            }

            tierStatsMap[tier] = TierCacheStats(
                tier = tier,
                itemCount = count,
                sizeBytes = bytes,
                maxSizeBytes = maxCap,
                hitCount = hits,
                missCount = misses
            )

            totalItems += count
            totalBytes += bytes
            totalHits += hits
            totalMisses += misses
        }

        val stats = ImageCacheStatsModel(
            totalItems = totalItems,
            totalSizeBytes = totalBytes,
            maxSizeBytes = 144L * 1024L * 1024L,
            totalHits = totalHits,
            totalMisses = totalMisses,
            tiers = tierStatsMap
        )

        _cacheStats.value = stats
        publishLoaderState()
    }

    private fun publishLoaderState() {
        val reqList = requestsMap.values.toList()
        val active = reqList.count { it.status == ImageLoadingStatus.LOADING || it.status == ImageLoadingStatus.QUEUED }
        val completed = reqList.count { it.status == ImageLoadingStatus.LOADED }
        val failed = reqList.count { it.status == ImageLoadingStatus.FAILED }

        _state.value = _state.value.copy(
            requests = reqList,
            activeCount = active,
            completedCount = completed,
            failedCount = failed,
            cacheStats = _cacheStats.value
        )
    }
}
