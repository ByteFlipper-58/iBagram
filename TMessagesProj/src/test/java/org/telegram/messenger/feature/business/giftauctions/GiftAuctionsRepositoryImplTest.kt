package org.telegram.messenger.feature.business.giftauctions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.GiftAuctionController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.data.datasource.GiftAuctionsLocalDataSource
import org.telegram.messenger.feature.business.giftauctions.data.datasource.GiftAuctionsRemoteDataSource
import org.telegram.messenger.feature.business.giftauctions.data.repository.GiftAuctionsRepositoryImpl
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionStatus
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Gifts.AuctionBidSheet

@OptIn(ExperimentalCoroutinesApi::class)
class GiftAuctionsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: FakeGiftAuctionsLocalDataSource
    private lateinit var remoteDataSource: FakeGiftAuctionsRemoteDataSource
    private lateinit var repository: GiftAuctionsRepositoryImpl

    private class FakeGiftAuctionsLocalDataSource(account: Int) : GiftAuctionsLocalDataSource(account) {
        var sendBidSuccess = true

        override fun getController(): GiftAuctionController? = null
        override fun getActiveAuctions(): List<GiftAuctionController.Auction> = emptyList()
        override fun getAuction(giftId: Long): GiftAuctionController.Auction? = null

        override fun sendBid(
            giftId: Long,
            params: AuctionBidSheet.Params?,
            amount: Long,
            callback: (Boolean, String?) -> Unit
        ) {
            callback(sendBidSuccess, if (sendBidSuccess) null else "BID_ERROR")
        }

        override fun subscribeToActiveAuctions(listener: GiftAuctionController.OnActiveAuctionsUpdateListeners) {
            listener.onActiveAuctionsUpdate(emptyList())
        }

        override fun unsubscribeFromActiveAuctions(listener: GiftAuctionController.OnActiveAuctionsUpdateListeners) {}

        override fun subscribeToAuction(
            giftId: Long,
            listener: GiftAuctionController.OnAuctionUpdateListener
        ): GiftAuctionController.Auction? = null

        override fun unsubscribeFromAuction(
            giftId: Long,
            listener: GiftAuctionController.OnAuctionUpdateListener
        ) {}
    }

    private class FakeGiftAuctionsRemoteDataSource(account: Int) : GiftAuctionsRemoteDataSource(account) {
        override suspend fun getAuctionById(giftId: Long): Result<TL_payments.TL_StarGiftAuctionState> {
            val state = TL_payments.TL_StarGiftAuctionState().apply {
                this.gift = TL_stars.TL_starGift().apply {
                    this.id = giftId
                    this.title = "Test Gift"
                }
            }
            return Result.Success(state)
        }

        override suspend fun getAuctionBySlug(slug: String): Result<TL_payments.TL_StarGiftAuctionState> {
            val state = TL_payments.TL_StarGiftAuctionState().apply {
                this.gift = TL_stars.TL_starGift().apply {
                    this.id = 100L
                    this.auction_slug = slug
                    this.title = "Slug Gift"
                }
            }
            return Result.Success(state)
        }

        override suspend fun loadAcquiredGifts(giftId: Long): Result<List<TL_stars.TL_StarGiftAuctionAcquiredGift>> {
            val gift = TL_stars.TL_StarGiftAuctionAcquiredGift().apply {
                this.gift_num = 1
                this.bid_amount = 500L
            }
            return Result.Success(listOf(gift))
        }

        override suspend fun requestActiveAuctions(): Result<Unit> {
            return Result.Success(Unit)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = FakeGiftAuctionsLocalDataSource(0)
        remoteDataSource = FakeGiftAuctionsRemoteDataSource(0)
        repository = GiftAuctionsRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetActiveAuctions() = runTest {
        val auctions = repository.getActiveAuctions()
        assertNotNull(auctions)
        assertTrue(auctions.isEmpty())
    }

    @Test
    fun testSendBidSuccess() = runTest {
        localDataSource.sendBidSuccess = true
        val result = repository.sendBid(42L, 1000L, null)
        assertTrue(result is Result.Success)
    }

    @Test
    fun testSendBidFailure() = runTest {
        localDataSource.sendBidSuccess = false
        val result = repository.sendBid(42L, 1000L, null)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun testLoadAcquiredGifts() = runTest {
        val result = repository.loadAcquiredGifts(42L)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(42L, list[0].giftId)
        assertEquals(500L, list[0].bidAmount)
    }

    @Test
    fun testRefreshActiveAuctions() = runTest {
        val result = repository.refreshActiveAuctions()
        assertTrue(result is Result.Success)
    }

    @Test
    fun testObserveActiveAuctions() = runTest {
        val firstEmitted = repository.observeActiveAuctions().first()
        assertNotNull(firstEmitted)
    }
}
