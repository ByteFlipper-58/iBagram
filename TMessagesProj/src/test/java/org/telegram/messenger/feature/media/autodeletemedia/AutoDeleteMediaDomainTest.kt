package org.telegram.messenger.feature.media.autodeletemedia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.AutoDeleteMediaTask
import org.telegram.messenger.feature.media.autodeletemedia.data.repository.LegacyAutoDeleteMediaRepository
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.CacheLimitConfig
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.MediaScanFileModel
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.CalculateEvictionCandidatesUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.CheckShouldRunCleanupUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.GetAutoDeleteStateUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.IsFileLockedUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.LockFileUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.ObserveAutoDeleteStateUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.RunAutoDeleteCleanupUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.UnlockFileUseCase
import org.telegram.messenger.feature.media.autodeletemedia.presentation.AutoDeleteMediaEvent
import org.telegram.messenger.feature.media.autodeletemedia.presentation.AutoDeleteMediaViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AutoDeleteMediaDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runCatching { AutoDeleteMediaTask.usingFilePaths.clear() }
    }

    @After
    fun tearDown() {
        runCatching { AutoDeleteMediaTask.usingFilePaths.clear() }
        Dispatchers.resetMain()
    }

    @Test
    fun testCheckShouldRunCleanupInterval() {
        val checkUseCase = CheckShouldRunCleanupUseCase()

        val now = 1_700_000_000L

        // Never checked before (lastCheckTime = 0) -> should run
        assertTrue(checkUseCase(currentTimeSec = now, lastCheckTimeSec = 0L))

        // Checked 10 hours ago (< 24h) -> should not run
        val tenHoursAgo = now - (10 * 3600L)
        assertFalse(checkUseCase(currentTimeSec = now, lastCheckTimeSec = tenHoursAgo))

        // Checked 25 hours ago (>= 24h) -> should run
        val twentyFiveHoursAgo = now - (25 * 3600L)
        assertTrue(checkUseCase(currentTimeSec = now, lastCheckTimeSec = twentyFiveHoursAgo))
    }

    @Test
    fun testCacheLimitConfigCalculation() {
        // Int.MAX_VALUE -> infinite cache
        assertEquals(Long.MAX_VALUE, CacheLimitConfig.calculateMaxCacheSizeBytes(Int.MAX_VALUE))

        // 1 GB preset -> 300 MB special Telegram threshold
        val expected300Mb = 1024L * 1024L * 300L
        assertEquals(expected300Mb, CacheLimitConfig.calculateMaxCacheSizeBytes(1))

        // 5 GB preset -> 5 * 1024 * 1024 * 1000 bytes
        val expected5Gb = 5L * 1024L * 1024L * 1000L
        assertEquals(expected5Gb, CacheLimitConfig.calculateMaxCacheSizeBytes(5))
    }

    @Test
    fun testCalculateEvictionCandidatesLru() {
        val calculateUseCase = CalculateEvictionCandidatesUseCase()

        val mb = 1024L * 1024L
        val file1 = MediaScanFileModel(path = "/file1.mp4", sizeBytes = 100 * mb, lastUsageTimeSec = 100)
        val file2 = MediaScanFileModel(path = "/file2.mp4", sizeBytes = 200 * mb, lastUsageTimeSec = 200)
        val file3Locked = MediaScanFileModel(path = "/file3.mp4", sizeBytes = 150 * mb, lastUsageTimeSec = 300, isLocked = true)
        val file4Forever = MediaScanFileModel(path = "/file4.mp4", sizeBytes = 100 * mb, lastUsageTimeSec = 400, isKeepForever = true)
        val file5 = MediaScanFileModel(path = "/file5.mp4", sizeBytes = 50 * mb, lastUsageTimeSec = 500)

        val allFiles = listOf(file1, file2, file3Locked, file4Forever, file5)
        val totalSize = allFiles.sumOf { it.sizeBytes }
        assertEquals(600 * mb, totalSize)

        // Evict until total size is <= 350 MB
        val plan = calculateUseCase(allFiles, maxCacheSizeBytes = 350 * mb)

        assertEquals(2, plan.filesToEvict.size)
        assertEquals("/file1.mp4", plan.filesToEvict[0].path)
        assertEquals("/file2.mp4", plan.filesToEvict[1].path)
        assertEquals(300 * mb, plan.bytesFreed)
        assertEquals(300 * mb, plan.remainingTotalBytes)
        assertTrue(plan.remainingTotalBytes <= 350 * mb)
    }

    @Test
    fun testFileLockingAndUnlocking() {
        val repository = LegacyAutoDeleteMediaRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val lockUseCase = LockFileUseCase(repository)
        val unlockUseCase = UnlockFileUseCase(repository)
        val isLockedUseCase = IsFileLockedUseCase(repository)

        val path = "/sdcard/Telegram/Video/important_video.mp4"

        assertFalse(isLockedUseCase(path))
        assertEquals(0, repository.getState().lockedFilesCount)

        lockUseCase(path)
        assertTrue(isLockedUseCase(path))
        assertEquals(1, repository.getState().lockedFilesCount)

        unlockUseCase(path)
        assertFalse(isLockedUseCase(path))
        assertEquals(0, repository.getState().lockedFilesCount)
    }

    @Test
    fun testAutoDeleteMediaViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyAutoDeleteMediaRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        val observeState = ObserveAutoDeleteStateUseCase(repository)
        val runCleanup = RunAutoDeleteCleanupUseCase(repository)
        val lockFile = LockFileUseCase(repository)
        val unlockFile = UnlockFileUseCase(repository)

        val viewModel = AutoDeleteMediaViewModel(
            observeAutoDeleteState = observeState,
            runAutoDeleteCleanup = runCleanup,
            lockFile = lockFile,
            unlockFile = unlockFile,
            repository = repository
        )

        advanceUntilIdle()

        // Initial state
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isCleaningUp)

        // Event: LockFile
        viewModel.onEvent(AutoDeleteMediaEvent.LockFile("/path/audio.ogg"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.taskState.lockedFilesCount)

        // Event: RunCleanup
        viewModel.onEvent(AutoDeleteMediaEvent.RunCleanup(force = true))
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertFalse(finalState.isCleaningUp)
        assertNotNull(finalState.lastResult)
        assertTrue(finalState.taskState.lastCheckTimeSec > 0L)
    }
}
