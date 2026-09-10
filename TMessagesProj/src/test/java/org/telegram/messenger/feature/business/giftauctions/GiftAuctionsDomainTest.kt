package org.telegram.messenger.feature.business.giftauctions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.data.mapper.GiftAuctionMapper
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionStatus
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionByIdUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionBySlugUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.LoadAuctionAcquiredGiftsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveAuctionUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.RefreshActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.SendAuctionBidUseCase
import org.telegram.messenger.feature.business.giftauctions.presentation.GiftAuctionsEvent
import org.telegram.messenger.feature.business.giftauctions.presentation.GiftAuctionsUiState
import org.telegram.messenger.feature.business.giftauctions.presentation.GiftAuctionsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class GiftAuctionsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGiftAuctionsRepository : GiftAuctionsRepository {
        val activeAuctionsList = mutableListOf<GiftAuctionModel>()
        val auctionsMap = mutableMapOf<Long, GiftAuctionModel>()
        val acquiredGiftsMap = mutableMapOf<Long, List<GiftAuctionAcquiredGiftModel>>()
        val bidsSent = mutableListOf<Pair<Long, Long>>()
        var shouldFail = false
        var refreshCount = 0

        private val activeAuctionsFlow = MutableStateFlow<List<GiftAuctionModel>>(emptyList())
        private val auctionFlows = mutableMapOf<Long, MutableStateFlow<GiftAuctionModel?>>()

        fun emitActive() {
            activeAuctionsFlow.value = activeAuctionsList.toList()
        }

        fun emitAuction(giftId: Long, model: GiftAuctionModel?) {
            if (model != null) {
                auctionsMap[giftId] = model
            } else {
                auctionsMap.remove(giftId)
            }
            auctionFlows.getOrPut(giftId) { MutableStateFlow(null) }.value = model
        }

        override fun observeActiveAuctions(): Flow<List<GiftAuctionModel>> = activeAuctionsFlow.asStateFlow()

        override fun observeAuction(giftId: Long): Flow<GiftAuctionModel?> {
            return auctionFlows.getOrPut(giftId) { MutableStateFlow(auctionsMap[giftId]) }.asStateFlow()
        }

        override suspend fun getActiveAuctions(): List<GiftAuctionModel> = activeAuctionsList.toList()

        override suspend fun getAuctionById(giftId: Long): Result<GiftAuctionModel> {
            if (shouldFail) {
                return Result.failure("Auction $giftId not found")
            }
            val auction = auctionsMap[giftId] ?: return Result.failure("Auction not found")
            return Result.Success(auction)
        }

        override suspend fun getAuctionBySlug(slug: String): Result<GiftAuctionModel> {
            if (shouldFail) {
                return Result.failure("Auction with slug $slug not found")
            }
            val auction = auctionsMap.values.find { it.slug == slug }
                ?: return Result.failure("Auction with slug $slug not found")
            return Result.Success(auction)
        }

        override suspend fun sendBid(
            giftId: Long,
            amount: Long,
            params: GiftAuctionBidParamsModel?
        ): Result<Unit> {
            if (shouldFail) {
                return Result.failure("Bid failed")
            }
            bidsSent.add(giftId to amount)
            val existing = auctionsMap[giftId]
            if (existing != null) {
                val updated = existing.copy(userBid = amount, userBidDate = 1000L)
                emitAuction(giftId, updated)
            }
            return Result.Success(Unit)
        }

        override suspend fun loadAcquiredGifts(giftId: Long): Result<List<GiftAuctionAcquiredGiftModel>> {
            if (shouldFail) {
                return Result.failure("Failed to load acquired gifts")
            }
            return Result.Success(acquiredGiftsMap[giftId] ?: emptyList())
        }

        override suspend fun refreshActiveAuctions(): Result<Unit> {
            refreshCount++
            if (shouldFail) {
                return Result.failure("Refresh failed")
            }
            emitActive()
            return Result.Success(Unit)
        }
    }

    @Test
    fun testGiftAuctionModelProperties() {
        val auction = GiftAuctionModel(
            giftId = 101L,
            title = "Exclusive Star Gift",
            slug = "exclusive-gift",
            status = GiftAuctionStatus.ACTIVE,
            currentBid = 500L,
            minBid = 100L,
            userBid = 200L,
            userBidDate = 1700000000L,
            endDate = 1700086400L,
            acquiredCount = 5,
            previewAttributes = listOf("Model", "Pattern"),
            acquiredGiftsCount = 3,
            isFinished = false
        )

        assertTrue(auction.hasUserBid)
        assertTrue(auction.isActive)
        assertFalse(auction.isFinished)

        val noUserBid = auction.copy(userBid = 0L, userBidDate = 0L)
        assertFalse(noUserBid.hasUserBid)

        val finishedAuction = auction.copy(status = GiftAuctionStatus.FINISHED, isFinished = true)
        assertFalse(finishedAuction.isActive)
        assertTrue(finishedAuction.isFinished)
    }

    @Test
    fun testGiftAuctionMapperBidParams() {
        val domainParams = GiftAuctionBidParamsModel(
            dialogId = 12345L,
            message = "Happy Birthday!",
            hideName = true
        )
        val legacyParams = GiftAuctionMapper.toLegacyBidParams(domainParams)
        assertNotNull(legacyParams)
        assertEquals(12345L, legacyParams?.dialogId)
        assertTrue(legacyParams?.hideName ?: false)
        assertEquals("Happy Birthday!", legacyParams?.message?.text)

        val nullParams = GiftAuctionMapper.toLegacyBidParams(null)
        assertNull(nullParams)

        assertNull(GiftAuctionMapper.mapAuction(null))
        assertNull(GiftAuctionMapper.mapAcquiredGift(101L, null))
    }

    @Test
    fun testUseCases() = runTest {
        val repo = FakeGiftAuctionsRepository()
        val observeActiveUseCase = ObserveActiveAuctionsUseCase(repo)
        val observeAuctionUseCase = ObserveAuctionUseCase(repo)
        val getActiveUseCase = GetActiveAuctionsUseCase(repo)
        val getByIdUseCase = GetAuctionByIdUseCase(repo)
        val getBySlugUseCase = GetAuctionBySlugUseCase(repo)
        val sendBidUseCase = SendAuctionBidUseCase(repo)
        val loadAcquiredGiftsUseCase = LoadAuctionAcquiredGiftsUseCase(repo)
        val refreshActiveUseCase = RefreshActiveAuctionsUseCase(repo)

        val sampleAuction = GiftAuctionModel(
            giftId = 1L,
            title = "Star Gift #1",
            slug = "gift-1",
            status = GiftAuctionStatus.ACTIVE,
            currentBid = 1000L,
            minBid = 500L
        )
        repo.activeAuctionsList.add(sampleAuction)
        repo.auctionsMap[1L] = sampleAuction
        repo.emitActive()
        repo.emitAuction(1L, sampleAuction)

        val activeList = getActiveUseCase()
        assertEquals(1, activeList.size)
        assertEquals(1L, activeList[0].giftId)

        val observedActive = observeActiveUseCase().first()
        assertEquals(1, observedActive.size)

        val byIdRes = getByIdUseCase(1L)
        assertTrue(byIdRes.isSuccess)
        assertEquals("Star Gift #1", byIdRes.getOrNull()?.title)

        val bySlugRes = getBySlugUseCase("gift-1")
        assertTrue(bySlugRes.isSuccess)
        assertEquals(1L, bySlugRes.getOrNull()?.giftId)

        val bidRes = sendBidUseCase(1L, 1500L, GiftAuctionBidParamsModel(dialogId = 0L))
        assertTrue(bidRes.isSuccess)
        assertEquals(1, repo.bidsSent.size)
        assertEquals(1500L, repo.bidsSent[0].second)

        val observedUpdated = observeAuctionUseCase(1L).first()
        assertEquals(1500L, observedUpdated?.userBid)
        assertTrue(observedUpdated?.hasUserBid == true)

        repo.acquiredGiftsMap[1L] = listOf(
            GiftAuctionAcquiredGiftModel(
                giftId = 1L,
                num = 1,
                date = 1700000000L,
                peerId = 999L,
                bidAmount = 1500L,
                round = 1,
                pos = 1,
                messageText = "Cheers!"
            )
        )
        val giftsRes = loadAcquiredGiftsUseCase(1L)
        assertTrue(giftsRes.isSuccess)
        assertEquals(1, giftsRes.getOrNull()?.size)
        assertEquals("Cheers!", giftsRes.getOrNull()?.get(0)?.messageText)

        val refreshRes = refreshActiveUseCase()
        assertTrue(refreshRes.isSuccess)
        assertEquals(1, repo.refreshCount)

        repo.shouldFail = true
        assertTrue(getByIdUseCase(1L).isFailure)
        assertTrue(getBySlugUseCase("gift-1").isFailure)
        assertTrue(sendBidUseCase(1L, 2000L).isFailure)
        assertTrue(loadAcquiredGiftsUseCase(1L).isFailure)
        assertTrue(refreshActiveUseCase().isFailure)
    }

    @Test
    fun testGiftAuctionsViewModel() = runTest {
        val repo = FakeGiftAuctionsRepository()
        val observeActiveUseCase = ObserveActiveAuctionsUseCase(repo)
        val observeAuctionUseCase = ObserveAuctionUseCase(repo)
        val getActiveUseCase = GetActiveAuctionsUseCase(repo)
        val getByIdUseCase = GetAuctionByIdUseCase(repo)
        val getBySlugUseCase = GetAuctionBySlugUseCase(repo)
        val sendBidUseCase = SendAuctionBidUseCase(repo)
        val loadAcquiredGiftsUseCase = LoadAuctionAcquiredGiftsUseCase(repo)
        val refreshActiveUseCase = RefreshActiveAuctionsUseCase(repo)

        val sampleAuction = GiftAuctionModel(
            giftId = 42L,
            title = "Mythic Star Gift",
            slug = "mythic-gift",
            status = GiftAuctionStatus.ACTIVE,
            currentBid = 3000L,
            minBid = 1000L
        )
        repo.activeAuctionsList.add(sampleAuction)
        repo.auctionsMap[42L] = sampleAuction
        repo.emitActive()
        repo.emitAuction(42L, sampleAuction)

        val vm = GiftAuctionsViewModel(
            observeActiveAuctionsUseCase = observeActiveUseCase,
            observeAuctionUseCase = observeAuctionUseCase,
            getActiveAuctionsUseCase = getActiveUseCase,
            getAuctionByIdUseCase = getByIdUseCase,
            getAuctionBySlugUseCase = getBySlugUseCase,
            sendAuctionBidUseCase = sendBidUseCase,
            loadAuctionAcquiredGiftsUseCase = loadAcquiredGiftsUseCase,
            refreshActiveAuctionsUseCase = refreshActiveUseCase
        )

        advanceUntilIdle()
        val state1 = vm.uiState.value
        assertTrue(state1 is GiftAuctionsUiState.Success)
        val success1 = state1 as GiftAuctionsUiState.Success
        assertEquals(1, success1.activeAuctions.size)
        assertTrue(success1.hasActiveAuctions)

        // Select auction
        vm.onEvent(GiftAuctionsEvent.SelectAuction(42L))
        advanceUntilIdle()
        val success2 = vm.uiState.value as GiftAuctionsUiState.Success
        assertEquals(42L, success2.selectedAuction?.giftId)

        // Send bid
        vm.onEvent(GiftAuctionsEvent.SendBid(42L, 3500L, GiftAuctionBidParamsModel(dialogId = 0L)))
        advanceUntilIdle()
        val success3 = vm.uiState.value as GiftAuctionsUiState.Success
        assertFalse(success3.isBidding)
        assertNull(success3.errorMessage)

        // Load auction by slug
        vm.onEvent(GiftAuctionsEvent.LoadAuctionBySlug("mythic-gift"))
        advanceUntilIdle()
        val success4 = vm.uiState.value as GiftAuctionsUiState.Success
        assertEquals("mythic-gift", success4.selectedAuction?.slug)

        // Load acquired gifts
        repo.acquiredGiftsMap[42L] = listOf(
            GiftAuctionAcquiredGiftModel(42L, 1, 1700000000L, 1001L, 3500L, 1, 1, "Awesome!")
        )
        vm.onEvent(GiftAuctionsEvent.LoadAcquiredGifts(42L))
        advanceUntilIdle()
        val success5 = vm.uiState.value as GiftAuctionsUiState.Success
        assertEquals(1, success5.acquiredGifts.size)

        // Refresh active auctions
        vm.onEvent(GiftAuctionsEvent.RefreshActiveAuctions)
        advanceUntilIdle()
        val success6 = vm.uiState.value as GiftAuctionsUiState.Success
        assertFalse(success6.isRefreshing)

        // Clear error
        vm.onEvent(GiftAuctionsEvent.ClearError)
        advanceUntilIdle()
        assertNull((vm.uiState.value as GiftAuctionsUiState.Success).errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.giftAuctionsRepository)
        assertNotNull(container.observeActiveAuctionsUseCase)
        assertNotNull(container.observeAuctionUseCase)
        assertNotNull(container.getActiveAuctionsUseCase)
        assertNotNull(container.getAuctionByIdUseCase)
        assertNotNull(container.getAuctionBySlugUseCase)
        assertNotNull(container.sendAuctionBidUseCase)
        assertNotNull(container.loadAuctionAcquiredGiftsUseCase)
        assertNotNull(container.refreshActiveAuctionsUseCase)
        assertNotNull(container.giftAuctionsViewModel)

        val fakeRepo = FakeGiftAuctionsRepository()
        container.giftAuctionsRepository = fakeRepo
        assertEquals(fakeRepo, container.giftAuctionsRepository)

        val newVm = container.createGiftAuctionsViewModel()
        assertNotNull(newVm)
    }
}
