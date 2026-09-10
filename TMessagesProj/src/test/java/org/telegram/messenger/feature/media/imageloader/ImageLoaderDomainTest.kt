package org.telegram.messenger.feature.media.imageloader

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.imageloader.data.mapper.ImageLoaderMapper
import org.telegram.messenger.feature.media.imageloader.data.repository.LegacyImageLoaderRepository
import org.telegram.messenger.feature.media.imageloader.domain.model.FrameExtractType
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageCacheTier
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageFilterSpec
import org.telegram.messenger.feature.media.imageloader.domain.model.ImageLoadingStatus
import org.telegram.messenger.feature.media.imageloader.domain.usecase.BuildImageCacheKeyUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.CalculateImageDownscaleUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.CancelImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ClearImageCacheUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.EnqueueImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.EvaluateImageCacheEligibilityUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.FormatImageFilterUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.GetImageLoaderStateUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ObserveImageLoaderStateUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ParseImageFilterUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.TrimImageMemoryUseCase
import org.telegram.messenger.feature.media.imageloader.presentation.ImageLoaderEvent
import org.telegram.messenger.feature.media.imageloader.presentation.ImageLoaderViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ImageLoaderDomainTest {

    private lateinit var repository: LegacyImageLoaderRepository
    private lateinit var parseFilterUseCase: ParseImageFilterUseCase
    private lateinit var formatFilterUseCase: FormatImageFilterUseCase
    private lateinit var buildKeyUseCase: BuildImageCacheKeyUseCase
    private lateinit var calculateDownscaleUseCase: CalculateImageDownscaleUseCase
    private lateinit var evaluateTierUseCase: EvaluateImageCacheEligibilityUseCase
    private lateinit var observeStateUseCase: ObserveImageLoaderStateUseCase
    private lateinit var getStateUseCase: GetImageLoaderStateUseCase
    private lateinit var enqueueRequestUseCase: EnqueueImageRequestUseCase
    private lateinit var cancelRequestUseCase: CancelImageRequestUseCase
    private lateinit var trimMemoryUseCase: TrimImageMemoryUseCase
    private lateinit var clearCacheUseCase: ClearImageCacheUseCase

    @Before
    fun setUp() {
        repository = LegacyImageLoaderRepository(currentAccount = 0)
        parseFilterUseCase = ParseImageFilterUseCase()
        formatFilterUseCase = FormatImageFilterUseCase()
        buildKeyUseCase = BuildImageCacheKeyUseCase()
        calculateDownscaleUseCase = CalculateImageDownscaleUseCase()
        evaluateTierUseCase = EvaluateImageCacheEligibilityUseCase()
        observeStateUseCase = ObserveImageLoaderStateUseCase(repository)
        getStateUseCase = GetImageLoaderStateUseCase(repository)
        enqueueRequestUseCase = EnqueueImageRequestUseCase(repository, evaluateTierUseCase, buildKeyUseCase)
        cancelRequestUseCase = CancelImageRequestUseCase(repository)
        trimMemoryUseCase = TrimImageMemoryUseCase(repository)
        clearCacheUseCase = ClearImageCacheUseCase(repository)
    }

    @Test
    fun testFilterStringParsingAndFormatting() {
        // Parse complex filter
        val spec = parseFilterUseCase("100_100_b_f")
        assertEquals(100, spec.width)
        assertEquals(100, spec.height)
        assertTrue(spec.isBlur)
        assertEquals(3, spec.blurRadius)
        assertTrue(spec.isWallpaper)
        assertFalse(spec.isRound)

        // Parse custom blur radius and round
        val spec2 = parseFilterUseCase("200_150_b8_r_exif")
        assertEquals(200, spec2.width)
        assertEquals(150, spec2.height)
        assertTrue(spec2.isBlur)
        assertEquals(8, spec2.blurRadius)
        assertTrue(spec2.isRound)
        assertTrue(spec2.checkExif)

        // Parse animation and frame flags
        val spec3 = parseFilterUseCase("g_firstframe_ignoreOrientation")
        assertTrue(spec3.isAutoplay)
        assertFalse(spec3.isAutoplayNonLoop)
        assertEquals(FrameExtractType.FIRST_FRAME, spec3.frameExtractType)
        assertTrue(spec3.ignoreOrientation)

        // Non-loop autoplay and lottie react
        val spec4 = parseFilterUseCase("gl_lastreactframe")
        assertTrue(spec4.isAutoplay)
        assertTrue(spec4.isAutoplayNonLoop)
        assertEquals(FrameExtractType.LAST_REACT_FRAME, spec4.frameExtractType)

        // Empty filter
        val emptySpec = parseFilterUseCase(null)
        assertEquals(0, emptySpec.width)
        assertEquals(0, emptySpec.height)
        assertFalse(emptySpec.isBlur)

        // Format back
        val formatted = formatFilterUseCase(
            ImageFilterSpec(
                width = 80,
                height = 80,
                isRound = true,
                isBlur = true,
                blurRadius = 5
            )
        )
        assertEquals("80_80_b5_r", formatted)
    }

    @Test
    fun testDownscaleCalculations() {
        // Source 1920x1080 -> Target 300x300
        // Half: 960x540 >= 300x300 -> sampleSize 2
        // Next half: 480x270 < 300 -> stops at 2
        val spec1 = calculateDownscaleUseCase(1920, 1080, 300, 300)
        assertEquals(2, spec1.sampleSize)
        assertEquals(960, spec1.scaledWidth)
        assertEquals(540, spec1.scaledHeight)

        // Source 4000x3000 -> Target 400x300
        // Half: 2000x1500 (>= target) -> 2
        // Half: 1000x750 (>= target) -> 4
        // Half: 500x375 (>= target) -> 8
        // Half: 250x187 (< target) -> stops at 8
        val spec2 = calculateDownscaleUseCase(4000, 3000, 400, 300)
        assertEquals(8, spec2.sampleSize)
        assertEquals(500, spec2.scaledWidth)
        assertEquals(375, spec2.scaledHeight)

        // Image smaller than target -> sampleSize 1
        val spec3 = calculateDownscaleUseCase(200, 200, 500, 500)
        assertEquals(1, spec3.sampleSize)
        assertEquals(200, spec3.scaledWidth)
        assertEquals(200, spec3.scaledHeight)

        // Zero dimensions edge case
        val specZero = calculateDownscaleUseCase(0, 0, 100, 100)
        assertEquals(1, specZero.sampleSize)
        assertEquals(0, specZero.scaledWidth)

        // Formatter test
        assertEquals("500 B", ImageLoaderMapper.formatBytes(500L))
        assertEquals("1.0 MB", ImageLoaderMapper.formatBytes(1024L * 1024L))
        assertEquals("2.5 GB", ImageLoaderMapper.formatBytes((2.5 * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun testCacheTierArbitrationAndKeyBuilding() {
        // Wallpaper spec -> WALLPAPER tier
        val wallpaperSpec = ImageFilterSpec(width = 1080, height = 1920, isWallpaper = true)
        assertEquals(ImageCacheTier.WALLPAPER, evaluateTierUseCase(wallpaperSpec))

        // Lottie / Frame spec -> LOTTIE tier
        val lottieSpec = ImageFilterSpec(frameExtractType = FrameExtractType.FIRST_FRAME)
        assertEquals(ImageCacheTier.LOTTIE, evaluateTierUseCase(lottieSpec))

        // Small thumbnail <= 100x100 -> SMALL tier
        val smallSpec = ImageFilterSpec(width = 80, height = 80)
        assertEquals(ImageCacheTier.SMALL, evaluateTierUseCase(smallSpec))

        // Small thumbnail with ignore cache flag -> DEFAULT tier
        val smallIgnoreSpec = ImageFilterSpec(width = 80, height = 80, isIgnoreCacheForSmall = true)
        assertEquals(ImageCacheTier.DEFAULT, evaluateTierUseCase(smallIgnoreSpec))

        // Standard image > 100x100 -> DEFAULT tier
        val normalSpec = ImageFilterSpec(width = 600, height = 400)
        assertEquals(ImageCacheTier.DEFAULT, evaluateTierUseCase(normalSpec))

        // Key building
        assertEquals("doc_123_100_100_b", buildKeyUseCase("doc_123", "100_100_b"))
        assertEquals("url_xyz", buildKeyUseCase("url_xyz", null))
        assertEquals("url_xyz", buildKeyUseCase("url_xyz", ""))
    }

    @Test
    fun testRequestQueueingDeduplicationAndCancellation() = runTest {
        val filterSpec = ImageFilterSpec(width = 200, height = 200)

        // 1. Enqueue when not in cache -> QUEUED and cache miss recorded
        val request = enqueueRequestUseCase(
            key = "photo_1001",
            url = "https://telegram.org/img1.jpg",
            filter = "200_200",
            filterSpec = filterSpec
        )
        assertEquals("photo_1001_200_200", request.key)
        assertEquals(ImageLoadingStatus.QUEUED, request.status)
        assertEquals(ImageCacheTier.DEFAULT, request.targetTier)

        var state = getStateUseCase()
        assertEquals(1, state.activeCount)
        assertEquals(0, state.completedCount)
        assertEquals(1, state.cacheStats.totalMisses)

        // 2. Progress update
        repository.updateRequestStatus(
            key = request.key,
            status = ImageLoadingStatus.LOADING,
            progress = 0.5f,
            loadedBytes = 50000L,
            totalBytes = 100000L
        )
        state = getStateUseCase()
        val inProgress = state.requests.first { it.key == request.key }
        assertEquals(ImageLoadingStatus.LOADING, inProgress.status)
        assertEquals(0.5f, inProgress.progress, 0.001f)

        // 3. Mark loaded & put into cache
        repository.updateRequestStatus(
            key = request.key,
            status = ImageLoadingStatus.LOADED,
            progress = 1.0f,
            loadedBytes = 100000L,
            totalBytes = 100000L
        )
        repository.putCacheEntry(request.key, 100000L, ImageCacheTier.DEFAULT)

        state = getStateUseCase()
        assertEquals(0, state.activeCount)
        assertEquals(1, state.completedCount)
        assertTrue(repository.hasInCache(request.key, ImageCacheTier.DEFAULT))

        // 4. Second enqueue of identical item -> returns instantly LOADED, records hit
        val cachedRequest = enqueueRequestUseCase(
            key = "photo_1001",
            url = "https://telegram.org/img1.jpg",
            filter = "200_200",
            filterSpec = filterSpec
        )
        assertEquals(ImageLoadingStatus.LOADED, cachedRequest.status)
        assertEquals(1.0f, cachedRequest.progress, 0.001f)

        state = getStateUseCase()
        assertEquals(1, state.cacheStats.totalHits)

        // 5. Cancel request
        val cancelReq = enqueueRequestUseCase(
            key = "photo_to_cancel",
            filter = "50_50",
            filterSpec = ImageFilterSpec(width = 50, height = 50)
        )
        val cancelled = cancelRequestUseCase(cancelReq.key)
        assertTrue(cancelled)

        state = getStateUseCase()
        val cancelledItem = state.requests.first { it.key == cancelReq.key }
        assertEquals(ImageLoadingStatus.CANCELLED, cancelledItem.status)
        assertTrue(cancelledItem.isFinished)
    }

    @Test
    fun testMemoryTrimmingAndViewModelMviEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = ImageLoaderViewModel(
            observeImageLoaderStateUseCase = observeStateUseCase,
            enqueueImageRequestUseCase = enqueueRequestUseCase,
            cancelImageRequestUseCase = cancelRequestUseCase,
            trimImageMemoryUseCase = trimMemoryUseCase,
            clearImageCacheUseCase = clearCacheUseCase,
            scope = testScope
        )

        testScheduler.advanceUntilIdle()
        val initialUiState = viewModel.uiState.value
        assertEquals(0, initialUiState.activeCount)

        // Put some items into various cache tiers
        repository.putCacheEntry("def1", 1000L, ImageCacheTier.DEFAULT)
        repository.putCacheEntry("def2", 2000L, ImageCacheTier.DEFAULT)
        repository.putCacheEntry("small1", 300L, ImageCacheTier.SMALL)
        repository.putCacheEntry("wall1", 5000L, ImageCacheTier.WALLPAPER)
        repository.putCacheEntry("lottie1", 800L, ImageCacheTier.LOTTIE)

        var stats = repository.getCacheStats()
        assertEquals(5, stats.totalItems)
        assertEquals(9100L, stats.totalSizeBytes)

        // Select cache tier event
        viewModel.onEvent(ImageLoaderEvent.SelectCacheTier(ImageCacheTier.WALLPAPER))
        testScheduler.advanceUntilIdle()
        assertEquals(ImageCacheTier.WALLPAPER, viewModel.uiState.value.selectedTier)

        // Clear wallpaper cache event
        viewModel.onEvent(ImageLoaderEvent.ClearCache(ImageCacheTier.WALLPAPER))
        testScheduler.advanceUntilIdle()
        assertFalse(repository.hasInCache("wall1", ImageCacheTier.WALLPAPER))
        assertTrue(repository.hasInCache("def1", ImageCacheTier.DEFAULT))

        stats = repository.getCacheStats()
        assertEquals(4, stats.totalItems)

        // Trim memory event (level 80 = TRIM_MEMORY_COMPLETE)
        viewModel.onEvent(ImageLoaderEvent.TrimMemory(level = 80))
        testScheduler.advanceUntilIdle()

        stats = repository.getCacheStats()
        assertEquals(0, stats.totalItems)
        assertEquals(0L, stats.totalSizeBytes)

        val stateAfterTrim = viewModel.uiState.value
        assertEquals(80, stateAfterTrim.lastTrimmedLevel)
        assertFalse(stateAfterTrim.isTrimming)
    }
}
