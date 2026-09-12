package org.telegram.messenger.feature.media.fileref

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.FileRefController
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefLocalDataSource
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefRemoteDataSource
import org.telegram.messenger.feature.media.fileref.data.repository.FileRefRepositoryImpl
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefParentType
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefRequestItem

@OptIn(ExperimentalCoroutinesApi::class)
class FileRefRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeFileRefLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeFileRefRemoteDataSource
    private lateinit var repository: FileRefRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeFileRefLocalDataSource()
        fakeRemoteDataSource = FakeFileRefRemoteDataSource()
        repository = FileRefRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStatsEmpty() {
        val stats = repository.getStats()
        assertEquals(0, stats.activeRequestsCount)
        assertEquals(0, stats.cachedResponsesCount)
        assertEquals(0, stats.pendingLocationsCount)
        assertEquals(0L, stats.totalRenewedCount)
    }

    @Test
    fun testRequestReferenceRenewalPending() = runTest {
        val item = FileRefRequestItem(
            locationKey = "loc_123",
            parentKey = "parent_abc",
            parentType = FileRefParentType.MESSAGE
        )

        val immediate = repository.requestReferenceRenewal(item)
        assertFalse(immediate)

        val stats = repository.getStats()
        assertEquals(1, stats.activeRequestsCount)
        assertEquals(1, stats.pendingLocationsCount)
        assertEquals(0, stats.cachedResponsesCount)
    }

    @Test
    fun testNotifyReferenceRenewed() = runTest {
        val item = FileRefRequestItem(
            locationKey = "loc_456",
            parentKey = "parent_def",
            parentType = FileRefParentType.CHAT
        )
        repository.requestReferenceRenewal(item)

        repository.notifyReferenceRenewed("loc_456", "parent_def", refLength = 64)

        val stats = repository.getStats()
        assertEquals(0, stats.activeRequestsCount)
        assertEquals(1, stats.cachedResponsesCount)
        assertEquals(1L, stats.totalRenewedCount)
        assertTrue(stats.lastRenewalTime > 0L)
    }

    @Test
    fun testRequestReferenceRenewalCached() = runTest {
        repository.notifyReferenceRenewed("loc_789", "parent_cached", refLength = 32)

        val item = FileRefRequestItem(
            locationKey = "loc_new",
            parentKey = "parent_cached",
            parentType = FileRefParentType.CHAT
        )
        val immediate = repository.requestReferenceRenewal(item)
        assertTrue(immediate)

        val stats = repository.getStats()
        assertEquals(0, stats.activeRequestsCount)
        assertEquals(1, stats.cachedResponsesCount)
        assertEquals(2L, stats.totalRenewedCount)
    }

    @Test
    fun testCancelPendingRequest() = runTest {
        val item = FileRefRequestItem(
            locationKey = "loc_to_cancel",
            parentKey = "parent_cancel",
            parentType = FileRefParentType.WALLPAPER
        )
        repository.requestReferenceRenewal(item)
        assertEquals(1, repository.getStats().activeRequestsCount)

        repository.cancelPendingRequest("loc_to_cancel")
        assertEquals(0, repository.getStats().activeRequestsCount)
    }

    @Test
    fun testClearCache() = runTest {
        repository.notifyReferenceRenewed("loc_1", "parent_1", refLength = 16)
        assertEquals(1, repository.getStats().cachedResponsesCount)

        repository.clearCache()
        assertEquals(0, repository.getStats().cachedResponsesCount)
        assertTrue(fakeLocalDataSource.memoryCleared)
    }

    @Test
    fun testStranglerHook() {
        val repo = FileRefController.getFileRefRepository(0)
        assertNotNull(repo)
    }

    // Fakes
    private class FakeFileRefLocalDataSource : FileRefLocalDataSource(0) {
        var memoryCleared = false

        override fun clearMemoryCache() {
            super.clearMemoryCache()
            memoryCleared = true
        }
    }

    private class FakeFileRefRemoteDataSource : FileRefRemoteDataSource(0)
}
