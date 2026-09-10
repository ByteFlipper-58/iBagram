package org.telegram.messenger.feature.media.downloadmanager

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
import org.telegram.messenger.feature.media.downloadmanager.data.mapper.DownloadManagerMapper
import org.telegram.messenger.feature.media.downloadmanager.data.repository.LegacyDownloadManagerRepository
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadMediaType
import org.telegram.messenger.feature.media.downloadmanager.domain.model.AutoDownloadNetwork
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemModel
import org.telegram.messenger.feature.media.downloadmanager.domain.model.DownloadItemStatus
import org.telegram.messenger.feature.media.downloadmanager.domain.model.PeerTypePreset
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.CalculateDownloadSpeedUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.CancelDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ClearRecentDownloadsUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.EnqueueDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.EvaluateAutoDownloadEligibilityUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.GetDownloadManagerStateUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.MarkDownloadsAsViewedUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ObserveDownloadManagerStateUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.PauseDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ResumeDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.RetryDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.SetDownloadNetworkTypeUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.UpdateDownloadPresetUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.UpdateDownloadProgressUseCase
import org.telegram.messenger.feature.media.downloadmanager.presentation.DownloadManagerEvent
import org.telegram.messenger.feature.media.downloadmanager.presentation.DownloadManagerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadManagerDomainTest {

    private lateinit var repository: LegacyDownloadManagerRepository
    private lateinit var evaluateEligibilityUseCase: EvaluateAutoDownloadEligibilityUseCase
    private lateinit var observeStateUseCase: ObserveDownloadManagerStateUseCase
    private lateinit var getStateUseCase: GetDownloadManagerStateUseCase
    private lateinit var enqueueDownloadUseCase: EnqueueDownloadUseCase
    private lateinit var pauseDownloadUseCase: PauseDownloadUseCase
    private lateinit var resumeDownloadUseCase: ResumeDownloadUseCase
    private lateinit var cancelDownloadUseCase: CancelDownloadUseCase
    private lateinit var retryDownloadUseCase: RetryDownloadUseCase
    private lateinit var clearRecentUseCase: ClearRecentDownloadsUseCase
    private lateinit var markViewedUseCase: MarkDownloadsAsViewedUseCase
    private lateinit var updateProgressUseCase: UpdateDownloadProgressUseCase
    private lateinit var speedCalcUseCase: CalculateDownloadSpeedUseCase
    private lateinit var setNetworkUseCase: SetDownloadNetworkTypeUseCase
    private lateinit var updatePresetUseCase: UpdateDownloadPresetUseCase

    @Before
    fun setUp() {
        repository = LegacyDownloadManagerRepository(currentAccount = 0)
        evaluateEligibilityUseCase = EvaluateAutoDownloadEligibilityUseCase(repository)
        observeStateUseCase = ObserveDownloadManagerStateUseCase(repository)
        getStateUseCase = GetDownloadManagerStateUseCase(repository)
        enqueueDownloadUseCase = EnqueueDownloadUseCase(repository)
        pauseDownloadUseCase = PauseDownloadUseCase(repository)
        resumeDownloadUseCase = ResumeDownloadUseCase(repository)
        cancelDownloadUseCase = CancelDownloadUseCase(repository)
        retryDownloadUseCase = RetryDownloadUseCase(repository)
        clearRecentUseCase = ClearRecentDownloadsUseCase(repository)
        markViewedUseCase = MarkDownloadsAsViewedUseCase(repository)
        updateProgressUseCase = UpdateDownloadProgressUseCase(repository)
        speedCalcUseCase = CalculateDownloadSpeedUseCase()
        setNetworkUseCase = SetDownloadNetworkTypeUseCase(repository)
        updatePresetUseCase = UpdateDownloadPresetUseCase(repository)
    }

    @Test
    fun testBitmaskEncodingAndPresetParsing() {
        // 1. Bitmask encoding
        val photoVideo = setOf(AutoDownloadMediaType.PHOTO, AutoDownloadMediaType.VIDEO)
        val mask = DownloadManagerMapper.encodeMediaMask(photoVideo)
        assertEquals(5, mask) // 1 (photo) | 4 (video)

        val decoded = DownloadManagerMapper.decodeMediaMask(mask)
        assertEquals(2, decoded.size)
        assertTrue(decoded.contains(AutoDownloadMediaType.PHOTO))
        assertTrue(decoded.contains(AutoDownloadMediaType.VIDEO))
        assertFalse(decoded.contains(AutoDownloadMediaType.AUDIO))

        // 2. Preset parsing from string
        // 15_15_15_15_512000_10485760_3145728_524288_1_1_1_0_1200_1
        val rawPreset = "15_15_15_15_512000_10485760_3145728_524288_1_1_1_0_1200_1"
        val parsed = DownloadManagerMapper.parsePresetString(rawPreset)
        assertTrue(parsed.enabled)
        assertTrue(parsed.preloadVideo)
        assertTrue(parsed.preloadMusic)
        assertTrue(parsed.preloadStories)
        assertFalse(parsed.lessCallData)
        assertEquals(1200, parsed.maxVideoBitrate)
        assertEquals(512000L, parsed.maxSizes[AutoDownloadMediaType.PHOTO])
        assertEquals(10485760L, parsed.maxSizes[AutoDownloadMediaType.VIDEO])

        // 3. Formatters with Locale.US
        assertEquals("1.0 MB", DownloadManagerMapper.formatFileSize(1024L * 1024L))
        assertEquals("500.0 KB/s", DownloadManagerMapper.formatSpeed(500L * 1024L))
        assertEquals("2.5 MB/s", DownloadManagerMapper.formatSpeed((2.5 * 1024L * 1024L).toLong()))
    }

    @Test
    fun testAutoDownloadEligibilityEvaluation() {
        // Default network is WIFI
        assertEquals(AutoDownloadNetwork.WIFI, getStateUseCase().currentNetwork)

        // Wi-Fi allows 10MB photo, 15MB video, 3MB document
        assertTrue(evaluateEligibilityUseCase(AutoDownloadMediaType.PHOTO, PeerTypePreset.CONTACTS, 5L * 1024L * 1024L))
        assertTrue(evaluateEligibilityUseCase(AutoDownloadMediaType.VIDEO, PeerTypePreset.GROUPS, 12L * 1024L * 1024L))
        assertFalse(evaluateEligibilityUseCase(AutoDownloadMediaType.VIDEO, PeerTypePreset.GROUPS, 20L * 1024L * 1024L))

        // Switch to CELLULAR
        setNetworkUseCase(AutoDownloadNetwork.CELLULAR)
        assertEquals(AutoDownloadNetwork.CELLULAR, getStateUseCase().currentNetwork)

        // Cellular allows up to 5MB video, rejects 8MB video
        assertTrue(evaluateEligibilityUseCase(AutoDownloadMediaType.VIDEO, PeerTypePreset.PRIVATE_CHATS, 4L * 1024L * 1024L))
        assertFalse(evaluateEligibilityUseCase(AutoDownloadMediaType.VIDEO, PeerTypePreset.PRIVATE_CHATS, 8L * 1024L * 1024L))

        // Switch to ROAMING (disabled by default)
        setNetworkUseCase(AutoDownloadNetwork.ROAMING)
        assertFalse(evaluateEligibilityUseCase(AutoDownloadMediaType.PHOTO, PeerTypePreset.CONTACTS, 100L * 1024L))
    }

    @Test
    fun testDownloadLifecycleAndSpeedCalculation() {
        // Speed calculation
        assertEquals(1048576L, speedCalcUseCase(1048576L, 1000L)) // 1MB/s
        assertEquals(2097152L, speedCalcUseCase(1048576L, 500L))  // 2MB/s
        assertEquals(0L, speedCalcUseCase(0L, 1000L))
        assertEquals(0L, speedCalcUseCase(100L, 0L))

        // Enqueue download
        val item = DownloadItemModel(
            id = "file_101",
            fileName = "archive.zip",
            sizeBytes = 10000000L, // 10MB
            dialogId = 12345L
        )
        assertTrue(enqueueDownloadUseCase(item))

        var state = getStateUseCase()
        assertEquals(1, state.downloadingFiles.size)
        assertEquals(DownloadItemStatus.QUEUED, state.downloadingFiles[0].status)
        assertEquals(1, state.stats.totalActiveDownloads)

        // Update progress to 50%
        updateProgressUseCase(item.id, 5000000L, 10000000L)
        state = getStateUseCase()
        val inProgress = state.downloadingFiles.first { it.id == item.id }
        assertEquals(DownloadItemStatus.DOWNLOADING, inProgress.status)
        assertEquals(0.5f, inProgress.progress, 0.001f)
        assertEquals(5000000L, inProgress.downloadedBytes)

        // Complete download
        repository.completeDownload(item.id, "/storage/archive.zip")
        state = getStateUseCase()
        assertEquals(0, state.downloadingFiles.size)
        assertEquals(1, state.recentDownloadingFiles.size)
        assertEquals(1, state.unviewedDownloads.size)
        assertEquals(1, state.stats.totalCompletedDownloads)
        assertEquals(1, state.stats.unviewedCount)
        assertEquals("/storage/archive.zip", state.recentDownloadingFiles[0].path)
        assertEquals(DownloadItemStatus.COMPLETED, state.recentDownloadingFiles[0].status)
    }

    @Test
    fun testPauseResumeRetryAndCancellation() {
        val item1 = DownloadItemModel(id = "f1", fileName = "test1.mp4", sizeBytes = 5000000L)
        val item2 = DownloadItemModel(id = "f2", fileName = "test2.mp3", sizeBytes = 2000000L)

        enqueueDownloadUseCase(item1)
        enqueueDownloadUseCase(item2)

        // Pause f1
        assertTrue(pauseDownloadUseCase("f1"))
        var state = getStateUseCase()
        val paused = state.downloadingFiles.first { it.id == "f1" }
        assertEquals(DownloadItemStatus.PAUSED, paused.status)

        // Resume f1
        assertTrue(resumeDownloadUseCase("f1"))
        state = getStateUseCase()
        val resumed = state.downloadingFiles.first { it.id == "f1" }
        assertEquals(DownloadItemStatus.DOWNLOADING, resumed.status)

        // Fail f2 then retry
        repository.failDownload("f2", canceled = false)
        state = getStateUseCase()
        assertEquals(DownloadItemStatus.FAILED, state.downloadingFiles.first { it.id == "f2" }.status)

        assertTrue(retryDownloadUseCase("f2"))
        state = getStateUseCase()
        assertEquals(DownloadItemStatus.DOWNLOADING, state.downloadingFiles.first { it.id == "f2" }.status)

        // Cancel f2
        assertTrue(cancelDownloadUseCase("f2"))
        state = getStateUseCase()
        assertFalse(state.downloadingFiles.any { it.id == "f2" })

        // Mark viewed
        repository.completeDownload("f1", "/path/f1")
        state = getStateUseCase()
        assertEquals(1, state.unviewedDownloads.size)

        markViewedUseCase()
        state = getStateUseCase()
        assertEquals(0, state.unviewedDownloads.size)
        assertEquals(1, state.recentDownloadingFiles.size)
        assertTrue(state.recentDownloadingFiles[0].isViewed)

        // Clear recent
        clearRecentUseCase()
        state = getStateUseCase()
        assertEquals(0, state.recentDownloadingFiles.size)
    }

    @Test
    fun testViewModelMviEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = DownloadManagerViewModel(
            observeDownloadManagerStateUseCase = observeStateUseCase,
            enqueueDownloadUseCase = enqueueDownloadUseCase,
            pauseDownloadUseCase = pauseDownloadUseCase,
            resumeDownloadUseCase = resumeDownloadUseCase,
            cancelDownloadUseCase = cancelDownloadUseCase,
            retryDownloadUseCase = retryDownloadUseCase,
            clearRecentDownloadsUseCase = clearRecentUseCase,
            markDownloadsAsViewedUseCase = markViewedUseCase,
            setDownloadNetworkTypeUseCase = setNetworkUseCase,
            updateDownloadPresetUseCase = updatePresetUseCase,
            scope = testScope
        )

        testScheduler.advanceUntilIdle()
        val initialUiState = viewModel.uiState.value
        assertEquals(AutoDownloadNetwork.WIFI, initialUiState.currentNetwork)

        // Enqueue via event
        val newItem = DownloadItemModel(id = "vm_doc", fileName = "manual.pdf", sizeBytes = 1024000L)
        viewModel.onEvent(DownloadManagerEvent.EnqueueDownload(newItem))
        testScheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertEquals(1, uiState.downloadingFiles.size)
        assertEquals("vm_doc", uiState.downloadingFiles[0].id)

        // Pause via event
        viewModel.onEvent(DownloadManagerEvent.PauseDownload("vm_doc"))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(DownloadItemStatus.PAUSED, uiState.downloadingFiles[0].status)

        // Set network type via event
        viewModel.onEvent(DownloadManagerEvent.SetNetworkType(AutoDownloadNetwork.CELLULAR))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(AutoDownloadNetwork.CELLULAR, uiState.currentNetwork)

        // Cancel via event
        viewModel.onEvent(DownloadManagerEvent.CancelDownload("vm_doc"))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(0, uiState.downloadingFiles.size)
    }
}
