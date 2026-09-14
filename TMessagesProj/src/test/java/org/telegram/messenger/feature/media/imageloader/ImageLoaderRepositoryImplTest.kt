package org.telegram.messenger.feature.media.imageloader

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.imageloader.data.datasource.ImageLoaderLocalDataSource
import org.telegram.messenger.feature.media.imageloader.data.datasource.ImageLoaderRemoteDataSource
import org.telegram.messenger.feature.media.imageloader.data.repository.ImageLoaderRepositoryImpl
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageRequestModel

class ImageLoaderRepositoryImplTest {

    private lateinit var localDataSource: ImageLoaderLocalDataSource
    private lateinit var remoteDataSource: ImageLoaderRemoteDataSource
    private lateinit var repository: ImageLoaderRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ImageLoaderLocalDataSource(0)
        remoteDataSource = ImageLoaderRemoteDataSource(0)
        repository = ImageLoaderRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun putCacheEntry_and_hasInCache_worksAcrossTiers() {
        assertFalse(repository.hasInCache("img_1", ImageCacheTier.DEFAULT))
        repository.putCacheEntry("img_1", 1024L, ImageCacheTier.DEFAULT)
        assertTrue(repository.hasInCache("img_1", ImageCacheTier.DEFAULT))
        assertFalse(repository.hasInCache("img_1", ImageCacheTier.WALLPAPER))

        repository.putCacheEntry("wall_1", 4096L, ImageCacheTier.WALLPAPER)
        assertTrue(repository.hasInCache("wall_1", ImageCacheTier.WALLPAPER))
    }

    @Test
    fun removeCacheEntry_removesCorrectItem() {
        repository.putCacheEntry("key_1", 2048L, ImageCacheTier.SMALL)
        assertTrue(repository.hasInCache("key_1", ImageCacheTier.SMALL))

        val removed = repository.removeCacheEntry("key_1", ImageCacheTier.SMALL)
        assertTrue(removed)
        assertFalse(repository.hasInCache("key_1", ImageCacheTier.SMALL))
    }

    @Test
    fun clearCache_clearsSpecificOrAllTiers() {
        repository.putCacheEntry("def_1", 100L, ImageCacheTier.DEFAULT)
        repository.putCacheEntry("small_1", 200L, ImageCacheTier.SMALL)

        repository.clearCache(ImageCacheTier.DEFAULT)
        assertFalse(repository.hasInCache("def_1", ImageCacheTier.DEFAULT))
        assertTrue(repository.hasInCache("small_1", ImageCacheTier.SMALL))

        repository.clearCache(null)
        assertFalse(repository.hasInCache("small_1", ImageCacheTier.SMALL))
    }

    @Test
    fun trimMemory_evictsTiersAccordingToLevel() {
        repository.putCacheEntry("def_1", 100L, ImageCacheTier.DEFAULT)
        repository.putCacheEntry("small_1", 200L, ImageCacheTier.SMALL)
        repository.putCacheEntry("lottie_1", 300L, ImageCacheTier.LOTTIE)

        // TRIM_MEMORY_BACKGROUND = 40
        repository.trimMemory(40)
        assertFalse(repository.hasInCache("def_1", ImageCacheTier.DEFAULT))
        assertFalse(repository.hasInCache("small_1", ImageCacheTier.SMALL))
        assertTrue(repository.hasInCache("lottie_1", ImageCacheTier.LOTTIE))

        // TRIM_MEMORY_COMPLETE = 80
        repository.trimMemory(80)
        assertFalse(repository.hasInCache("lottie_1", ImageCacheTier.LOTTIE))
    }

    @Test
    fun enqueueRequest_and_cancelRequest_updatesState() = runBlocking {
        val req = ImageRequestModel(
            key = "avatar_123",
            url = "https://example.com/avatar.jpg",
            targetTier = ImageCacheTier.SMALL
        )
        assertTrue(repository.enqueueRequest(req))

        val state = repository.observeState().first()
        assertEquals(1, state.requests.size)
        assertEquals(ImageLoadingStatus.QUEUED, state.requests.first().status)
        assertEquals(1, state.activeCount)

        assertTrue(repository.cancelRequest("avatar_123"))
        val stateAfter = repository.getState()
        assertEquals(ImageLoadingStatus.CANCELLED, stateAfter.requests.first().status)
    }

    @Test
    fun recordCacheHit_and_Miss_updatesStats() {
        repository.recordCacheHit(ImageCacheTier.DEFAULT)
        repository.recordCacheHit(ImageCacheTier.DEFAULT)
        repository.recordCacheMiss(ImageCacheTier.DEFAULT)

        val stats = repository.getCacheStats()
        assertEquals(2, stats.totalHits)
        assertEquals(1, stats.totalMisses)
        assertEquals(2f / 3f, stats.hitRate, 0.01f)
    }

    @Test
    fun updateRequestStatus_reflectsInState() {
        repository.enqueueRequest(ImageRequestModel(key = "photo_1"))
        repository.updateRequestStatus("photo_1", ImageLoadingStatus.LOADED, 1.0f, 5000L, 5000L)

        val state = repository.getState()
        assertEquals(1, state.completedCount)
        val r = state.requests.first { it.key == "photo_1" }
        assertEquals(ImageLoadingStatus.LOADED, r.status)
        assertEquals(5000L, r.loadedBytes)
    }
}
