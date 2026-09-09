package org.telegram.messenger.feature.fileref

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.fileref.data.repository.LegacyFileRefRepository
import org.telegram.messenger.feature.fileref.domain.model.FileRefCacheEntry
import org.telegram.messenger.feature.fileref.domain.model.FileRefParentType
import org.telegram.messenger.feature.fileref.domain.model.FileRefRequestItem
import org.telegram.messenger.feature.fileref.domain.usecase.CancelFileRefRequestUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.ClearFileRefCacheUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.GetFileRefStatsUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.NotifyReferenceRenewedUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.ObserveFileRefStatsUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.RequestReferenceRenewalUseCase
import org.telegram.messenger.feature.fileref.presentation.FileRefEvent
import org.telegram.messenger.feature.fileref.presentation.FileRefViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class FileRefDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyFileRefRepository
    private lateinit var observeFileRefStatsUseCase: ObserveFileRefStatsUseCase
    private lateinit var getFileRefStatsUseCase: GetFileRefStatsUseCase
    private lateinit var requestReferenceRenewalUseCase: RequestReferenceRenewalUseCase
    private lateinit var notifyReferenceRenewedUseCase: NotifyReferenceRenewedUseCase
    private lateinit var cancelFileRefRequestUseCase: CancelFileRefRequestUseCase
    private lateinit var clearFileRefCacheUseCase: ClearFileRefCacheUseCase
    private lateinit var viewModel: FileRefViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyFileRefRepository(account = 0)
        observeFileRefStatsUseCase = ObserveFileRefStatsUseCase(repository)
        getFileRefStatsUseCase = GetFileRefStatsUseCase(repository)
        requestReferenceRenewalUseCase = RequestReferenceRenewalUseCase(repository)
        notifyReferenceRenewedUseCase = NotifyReferenceRenewedUseCase(repository)
        cancelFileRefRequestUseCase = CancelFileRefRequestUseCase(repository)
        clearFileRefCacheUseCase = ClearFileRefCacheUseCase(repository)

        viewModel = FileRefViewModel(
            observeFileRefStatsUseCase = observeFileRefStatsUseCase,
            getFileRefStatsUseCase = getFileRefStatsUseCase,
            requestReferenceRenewalUseCase = requestReferenceRenewalUseCase,
            notifyReferenceRenewedUseCase = notifyReferenceRenewedUseCase,
            cancelFileRefRequestUseCase = cancelFileRefRequestUseCase,
            clearFileRefCacheUseCase = clearFileRefCacheUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFileRefCacheEntryExpiration() {
        val now = 1_000_000L
        val entry = FileRefCacheEntry(
            parentKey = "msg_123",
            timestamp = now,
            fileReferenceBytesCount = 32
        )

        // 30 seconds later: not expired
        assertFalse(entry.isExpired(now = now + 30_000L))

        // exactly 60 seconds later: not expired (<= 60_000)
        assertFalse(entry.isExpired(now = now + 60_000L))

        // 61 seconds later: expired
        assertTrue(entry.isExpired(now = now + 61_000L))

        // custom TTL: 10 seconds
        assertTrue(entry.isExpired(now = now + 15_000L, ttlMs = 10_000L))
        assertFalse(entry.isExpired(now = now + 5_000L, ttlMs = 10_000L))
    }

    @Test
    fun testFileRefRequestItemModels() {
        val item = FileRefRequestItem(
            locationKey = "loc_doc_999",
            parentKey = "chat_456",
            parentType = FileRefParentType.CHAT,
            requestTime = 12345L
        )

        assertEquals("loc_doc_999", item.locationKey)
        assertEquals("chat_456", item.parentKey)
        assertEquals(FileRefParentType.CHAT, item.parentType)
        assertEquals(12345L, item.requestTime)
    }

    @Test
    fun testRenewalWorkflowAndCacheHit() {
        val statsInitial = getFileRefStatsUseCase()
        assertEquals(0, statsInitial.activeRequestsCount)
        assertEquals(0, statsInitial.cachedResponsesCount)
        assertEquals(0L, statsInitial.totalRenewedCount)

        val item1 = FileRefRequestItem(
            locationKey = "loc_photo_1",
            parentKey = "msg_100",
            parentType = FileRefParentType.MESSAGE
        )

        // Initial request -> not in cache -> returns false (queued)
        val hit1 = requestReferenceRenewalUseCase(item1)
        assertFalse(hit1)

        val statsQueued = getFileRefStatsUseCase()
        assertEquals(1, statsQueued.activeRequestsCount)
        assertEquals(1, statsQueued.pendingLocationsCount)
        assertEquals(0, statsQueued.cachedResponsesCount)

        // Notify renewed
        notifyReferenceRenewedUseCase("loc_photo_1", "msg_100", refLength = 64)

        val statsRenewed = getFileRefStatsUseCase()
        assertEquals(0, statsRenewed.activeRequestsCount)
        assertEquals(1, statsRenewed.cachedResponsesCount)
        assertEquals(1L, statsRenewed.totalRenewedCount)
        assertTrue(statsRenewed.lastRenewalTime > 0L)

        // Request another item with SAME parentKey -> should hit cache immediately!
        val item2 = FileRefRequestItem(
            locationKey = "loc_photo_2",
            parentKey = "msg_100",
            parentType = FileRefParentType.MESSAGE
        )
        val hit2 = requestReferenceRenewalUseCase(item2)
        assertTrue(hit2)

        val statsAfterHit = getFileRefStatsUseCase()
        assertEquals(0, statsAfterHit.activeRequestsCount)
        assertEquals(1, statsAfterHit.cachedResponsesCount)
        assertEquals(2L, statsAfterHit.totalRenewedCount)
    }

    @Test
    fun testCancelPendingRequest() {
        val item = FileRefRequestItem(
            locationKey = "loc_cancel_1",
            parentKey = "user_200",
            parentType = FileRefParentType.USER
        )

        requestReferenceRenewalUseCase(item)
        assertEquals(1, getFileRefStatsUseCase().activeRequestsCount)

        cancelFileRefRequestUseCase("loc_cancel_1")
        assertEquals(0, getFileRefStatsUseCase().activeRequestsCount)
    }

    @Test
    fun testClearCache() {
        notifyReferenceRenewedUseCase("loc_temp", "parent_wallpaper", 128)
        assertEquals(1, getFileRefStatsUseCase().cachedResponsesCount)

        clearFileRefCacheUseCase()
        assertEquals(0, getFileRefStatsUseCase().cachedResponsesCount)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.stats.activeRequestsCount)

        val item = FileRefRequestItem(
            locationKey = "loc_vm_1",
            parentKey = "story_300",
            parentType = FileRefParentType.STORY
        )

        // Queue renewal via VM
        viewModel.onEvent(FileRefEvent.RequestRenewal(item))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.stats.activeRequestsCount)
        assertNull(viewModel.uiState.value.infoMessage)

        // Notify renewed
        viewModel.onEvent(FileRefEvent.NotifyRenewed("loc_vm_1", "story_300", 256))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.stats.activeRequestsCount)
        assertEquals(1, viewModel.uiState.value.stats.cachedResponsesCount)
        assertEquals(1L, viewModel.uiState.value.stats.totalRenewedCount)

        // Request again for same story -> immediate cache renewal with info message
        viewModel.onEvent(FileRefEvent.RequestRenewal(item.copy(locationKey = "loc_vm_2")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.infoMessage)
        assertTrue(viewModel.uiState.value.infoMessage!!.contains("Renewed immediately from cache"))

        // Dismiss info
        viewModel.onEvent(FileRefEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)

        // Clear cache
        viewModel.onEvent(FileRefEvent.ClearCache)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.stats.cachedResponsesCount)
        assertEquals("FileRef cache cleared", viewModel.uiState.value.infoMessage)
    }
}
