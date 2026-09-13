package org.telegram.messenger.feature.business.stargifts

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.stargifts.data.datasource.StarGiftsLocalDataSource
import org.telegram.messenger.feature.business.stargifts.data.datasource.StarGiftsRemoteDataSource
import org.telegram.messenger.feature.business.stargifts.data.repository.StarGiftsRepositoryImpl
import org.telegram.messenger.feature.business.stargifts.domain.model.StarGiftFilter
import org.telegram.tgnet.tl.TL_stars

class StarGiftsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: StarGiftsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeStarGiftsRemoteDataSource
    private lateinit var repository: StarGiftsRepositoryImpl

    private class FakeStarGiftsRemoteDataSource(account: Int) : StarGiftsRemoteDataSource(account) {
        var remoteCatalog: List<TL_stars.StarGift> = emptyList()
        var remoteGift: TL_stars.StarGift? = null
        var remoteSavedGifts: TL_stars.TL_payments_savedStarGifts? = null
        var shouldFail = false

        override suspend fun fetchCatalog(forceRefresh: Boolean): Result<List<TL_stars.StarGift>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteCatalog)
        }

        override suspend fun fetchGift(giftId: Long): Result<TL_stars.StarGift?> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteGift)
        }

        override suspend fun fetchProfileGifts(
            dialogId: Long,
            offset: String?,
            limit: Int,
            filter: StarGiftFilter
        ): Result<TL_stars.TL_payments_savedStarGifts> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteSavedGifts ?: TL_stars.TL_payments_savedStarGifts())
        }

        override suspend fun togglePinGift(dialogId: Long, giftId: Long, pin: Boolean): Result<Boolean> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(true)
        }

        override suspend fun toggleHideGift(dialogId: Long, giftId: Long, hide: Boolean): Result<Boolean> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(true)
        }
    }

    @Before
    fun setUp() {
        localDataSource = StarGiftsLocalDataSource(0).apply {
            setTestMode(true)
        }
        fakeRemoteDataSource = FakeStarGiftsRemoteDataSource(0)
        repository = StarGiftsRepositoryImpl(0, localDataSource, fakeRemoteDataSource, testDispatcher)
    }

    private fun createStarGift(id: Long, stars: Long, title: String): TL_stars.StarGift {
        return TL_stars.TL_starGift().apply {
            this.id = id
            this.stars = stars
            this.slug = "gift_$id"
            this.title = title
            this.limited = false
            this.sold_out = false
            this.birthday = false
        }
    }

    @Test
    fun testGetCatalogFromCache() = runTest(testDispatcher) {
        val gift1 = createStarGift(1L, 50L, "Gift 1")
        val gift2 = createStarGift(2L, 100L, "Gift 2")
        localDataSource.setTestCatalog(listOf(gift1, gift2))

        val result = repository.getCatalog(forceRefresh = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertEquals(1L, list[0].id)
        assertEquals(50L, list[0].stars)
        assertEquals("Gift 1", list[0].title)
    }

    @Test
    fun testGetCatalogFromRemoteWhenCacheEmpty() = runTest(testDispatcher) {
        val gift = createStarGift(3L, 200L, "Remote Gift")
        fakeRemoteDataSource.remoteCatalog = listOf(gift)

        val result = repository.getCatalog(forceRefresh = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(3L, list[0].id)
        assertEquals("Remote Gift", list[0].title)
    }

    @Test
    fun testGetGiftById() = runTest(testDispatcher) {
        val gift = createStarGift(42L, 500L, "Special Gift")
        localDataSource.setTestGift(gift)

        val result = repository.getGift(42L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data)
        assertEquals(42L, data?.id)
        assertEquals("Special Gift", data?.title)
    }

    @Test
    fun testLoadProfileGifts() = runTest(testDispatcher) {
        val saved = TL_stars.TL_savedStarGift().apply {
            this.saved_id = 1001L
            this.date = 1700000000
            this.gift = createStarGift(10L, 75L, "Profile Gift")
            this.pinned_to_top = true
            this.unsaved = false
        }
        val response = TL_stars.TL_payments_savedStarGifts().apply {
            this.gifts = arrayListOf(saved)
            this.count = 1
            this.next_offset = null
        }
        fakeRemoteDataSource.remoteSavedGifts = response

        val result = repository.loadProfileGifts(12345L, null, 30, StarGiftFilter())
        assertTrue(result is Result.Success)
        val model = (result as Result.Success).data
        assertEquals(12345L, model.dialogId)
        assertEquals(1, model.gifts.size)
        assertEquals(1001L, model.gifts[0].id)
        assertTrue(model.gifts[0].isPinnedToTop)
        assertEquals("Profile Gift", model.gifts[0].gift.title)
    }

    @Test
    fun testTogglePinAndHideGift() = runTest(testDispatcher) {
        val saved = TL_stars.TL_savedStarGift().apply {
            this.saved_id = 2002L
            this.date = 1700000000
            this.gift = createStarGift(20L, 100L, "Toggled Gift")
            this.pinned_to_top = false
            this.unsaved = false
        }
        localDataSource.setTestProfileGifts(555L, listOf(saved))

        val pinResult = repository.togglePinGift(555L, 2002L, true)
        assertTrue(pinResult is Result.Success)
        assertTrue((pinResult as Result.Success).data)

        val hideResult = repository.toggleHideGift(555L, 2002L, true)
        assertTrue(hideResult is Result.Success)
        assertTrue((hideResult as Result.Success).data)
    }

    @Test
    fun testObserveCatalog() = runTest(testDispatcher) {
        val gift = createStarGift(99L, 300L, "Observed Gift")
        localDataSource.setTestCatalog(listOf(gift))

        val emitted = repository.observeCatalog().first()
        assertEquals(1, emitted.gifts.size)
        assertEquals(99L, emitted.gifts[0].id)
    }
}
