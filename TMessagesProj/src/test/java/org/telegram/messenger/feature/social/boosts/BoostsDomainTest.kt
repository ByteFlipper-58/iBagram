package org.telegram.messenger.feature.social.boosts

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.ChannelBoostsController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.data.mapper.BoostMapper
import org.telegram.messenger.feature.social.boosts.domain.model.BoostSlotModel
import org.telegram.messenger.feature.social.boosts.domain.model.BoostStatusModel
import org.telegram.messenger.feature.social.boosts.domain.model.CanApplyBoostModel
import org.telegram.messenger.feature.social.boosts.domain.model.MyBoostsModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository
import org.telegram.messenger.feature.social.boosts.domain.usecase.ApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.CheckCanApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetBoostsStatusUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetMyBoostsUseCase
import org.telegram.messenger.feature.social.boosts.presentation.BoostsEvent
import org.telegram.messenger.feature.social.boosts.presentation.BoostsViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stats
import org.telegram.tgnet.tl.TL_stories

@OptIn(ExperimentalCoroutinesApi::class)
class BoostsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDomainModels() {
        val status = BoostStatusModel(
            level = 3,
            currentLevelBoosts = 10,
            boosts = 15,
            giftBoosts = 5,
            nextLevelBoosts = 20,
            premiumAudiencePart = 12.5,
            premiumAudienceTotal = 100.0,
            boostUrl = "https://t.me/boost/testchannel",
            hasMyBoost = true,
            myBoostSlots = listOf(1, 2),
            isMaxLevel = false
        )

        assertEquals(3, status.level)
        assertEquals(10, status.currentLevelBoosts)
        assertEquals(15, status.boosts)
        assertEquals(5, status.giftBoosts)
        assertEquals(20, status.nextLevelBoosts)
        assertEquals(12.5, status.premiumAudiencePart, 0.001)
        assertEquals(100.0, status.premiumAudienceTotal, 0.001)
        assertEquals("https://t.me/boost/testchannel", status.boostUrl)
        assertTrue(status.hasMyBoost)
        assertEquals(listOf(1, 2), status.myBoostSlots)
        assertFalse(status.isMaxLevel)

        val slot = BoostSlotModel(
            slot = 1,
            peerDialogId = -1001234567890L,
            date = 1700000000,
            expires = 1702592000,
            cooldownUntilDate = 0
        )

        assertEquals(1, slot.slot)
        assertEquals(-1001234567890L, slot.peerDialogId)
        assertEquals(1700000000, slot.date)
        assertEquals(1702592000, slot.expires)
        assertEquals(0, slot.cooldownUntilDate)

        val myBoosts = MyBoostsModel(listOf(slot))
        assertEquals(1, myBoosts.slots.size)
        assertEquals(1, myBoosts.slots[0].slot)

        val canApply = CanApplyBoostModel(
            canApply = true,
            empty = false,
            replaceDialogId = 0L,
            alreadyActive = false,
            needSelector = false,
            floodWait = 0,
            slot = 2,
            boostCount = 0,
            isMaxLevel = false
        )

        assertTrue(canApply.canApply)
        assertFalse(canApply.empty)
        assertEquals(0L, canApply.replaceDialogId)
        assertFalse(canApply.alreadyActive)
        assertFalse(canApply.needSelector)
        assertEquals(0, canApply.floodWait)
        assertEquals(2, canApply.slot)
        assertEquals(0, canApply.boostCount)
        assertFalse(canApply.isMaxLevel)
    }

    @Test
    fun testBoostMapper() {
        val tlStatus = TL_stories.TL_premium_boostsStatus().apply {
            level = 2
            current_level_boosts = 5
            boosts = 8
            gift_boosts = 3
            next_level_boosts = 0 // max level
            premium_audience = TL_stats.TL_statsPercentValue().apply {
                part = 5.0
                total = 50.0
            }
            boost_url = "https://t.me/boost/channel"
            my_boost = true
            my_boost_slots.add(1)
        }

        val mappedStatus = BoostMapper.mapToBoostStatus(tlStatus)
        assertEquals(2, mappedStatus.level)
        assertEquals(5, mappedStatus.currentLevelBoosts)
        assertEquals(8, mappedStatus.boosts)
        assertEquals(3, mappedStatus.giftBoosts)
        assertEquals(0, mappedStatus.nextLevelBoosts)
        assertTrue(mappedStatus.isMaxLevel)
        assertEquals(5.0, mappedStatus.premiumAudiencePart, 0.001)
        assertEquals(50.0, mappedStatus.premiumAudienceTotal, 0.001)
        assertEquals("https://t.me/boost/channel", mappedStatus.boostUrl)
        assertTrue(mappedStatus.hasMyBoost)
        assertEquals(listOf(1), mappedStatus.myBoostSlots)

        val tlBoost = TL_stories.TL_myBoost().apply {
            slot = 3
            date = 100
            expires = 200
            cooldown_until_date = 50
            peer = TLRPC.TL_peerChannel().apply { channel_id = 987654L }
        }

        val mappedSlot = BoostMapper.mapToBoostSlot(tlBoost)
        assertEquals(3, mappedSlot.slot)
        assertEquals(100, mappedSlot.date)
        assertEquals(200, mappedSlot.expires)
        assertEquals(50, mappedSlot.cooldownUntilDate)
        assertEquals(-987654L, mappedSlot.peerDialogId)

        val tlMyBoosts = TL_stories.TL_premium_myBoosts().apply {
            my_boosts.add(tlBoost)
        }
        val mappedMyBoosts = BoostMapper.mapToMyBoosts(tlMyBoosts)
        assertEquals(1, mappedMyBoosts.slots.size)
        assertEquals(3, mappedMyBoosts.slots[0].slot)

        val canApplyBoost = ChannelBoostsController.CanApplyBoost().apply {
            canApply = true
            empty = false
            replaceDialogId = -111L
            alreadyActive = true
            needSelector = true
            floodWait = 0
            slot = 1
            boostCount = 2
            isMaxLvl = false
        }
        val mappedCanApply = BoostMapper.mapToCanApplyBoost(canApplyBoost)
        assertTrue(mappedCanApply.canApply)
        assertFalse(mappedCanApply.empty)
        assertEquals(-111L, mappedCanApply.replaceDialogId)
        assertTrue(mappedCanApply.alreadyActive)
        assertTrue(mappedCanApply.needSelector)
        assertEquals(1, mappedCanApply.slot)
        assertEquals(2, mappedCanApply.boostCount)
        assertFalse(mappedCanApply.isMaxLevel)
    }

    @Test
    fun testBoostsUseCases() = runTest(testDispatcher) {
        val fakeRepo = FakeBoostsRepository()

        val getStatusUseCase = GetBoostsStatusUseCase(fakeRepo)
        val getMyBoostsUseCase = GetMyBoostsUseCase(fakeRepo)
        val checkCanApplyUseCase = CheckCanApplyBoostUseCase(fakeRepo)
        val applyBoostUseCase = ApplyBoostUseCase(fakeRepo)

        val statusResult = getStatusUseCase(-100L)
        assertTrue(statusResult is Result.Success)
        assertEquals(1, (statusResult as Result.Success).data.level)

        val myBoostsResult = getMyBoostsUseCase()
        assertTrue(myBoostsResult is Result.Success)
        assertEquals(1, (myBoostsResult as Result.Success).data.slots.size)

        val canApplyResult = checkCanApplyUseCase(-100L)
        assertTrue(canApplyResult is Result.Success)
        assertTrue((canApplyResult as Result.Success).data.canApply)

        val applyResult = applyBoostUseCase(-100L, listOf(1))
        assertTrue(applyResult is Result.Success)
        assertEquals(2, (applyResult as Result.Success).data.slots.size)

        // Test error handling
        fakeRepo.shouldFail = true
        val failedResult = getStatusUseCase(-100L)
        assertTrue(failedResult is Result.Failure)
        assertEquals("Repository failure", (failedResult as Result.Failure).error.message)
    }

    @Test
    fun testBoostsViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeBoostsRepository()
        val viewModel = BoostsViewModel(
            getBoostsStatusUseCase = GetBoostsStatusUseCase(fakeRepo),
            getMyBoostsUseCase = GetMyBoostsUseCase(fakeRepo),
            checkCanApplyBoostUseCase = CheckCanApplyBoostUseCase(fakeRepo),
            applyBoostUseCase = ApplyBoostUseCase(fakeRepo)
        )

        assertNull(viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isLoading)

        // 1. LoadStatus
        viewModel.onEvent(BoostsEvent.LoadStatus(-100L))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.status)
        assertEquals(1, viewModel.uiState.value.status?.level)
        assertFalse(viewModel.uiState.value.isLoading)

        // 2. LoadMyBoosts
        viewModel.onEvent(BoostsEvent.LoadMyBoosts)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.myBoosts)
        assertEquals(1, viewModel.uiState.value.myBoosts?.slots?.size)

        // 3. CheckCanApply
        viewModel.onEvent(BoostsEvent.CheckCanApply(-100L))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.canApply)
        assertTrue(viewModel.uiState.value.canApply?.canApply == true)

        // 4. ApplyBoost
        viewModel.onEvent(BoostsEvent.ApplyBoost(-100L, listOf(1)))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isApplying)
        assertEquals("Boost applied successfully", viewModel.uiState.value.actionSuccessMessage)
        assertEquals(2, viewModel.uiState.value.myBoosts?.slots?.size)

        // 5. ClearMessages
        viewModel.onEvent(BoostsEvent.ClearMessages)
        assertNull(viewModel.uiState.value.actionSuccessMessage)

        // 6. Error handling
        fakeRepo.shouldFail = true
        viewModel.onEvent(BoostsEvent.LoadStatus(-100L))
        advanceUntilIdle()
        assertEquals("Repository failure", viewModel.uiState.value.errorMessage)
    }

    private class FakeBoostsRepository : BoostsRepository {
        var shouldFail = false

        override suspend fun getBoostsStatus(dialogId: Long): Result<BoostStatusModel> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository failure"))
            return Result.Success(
                BoostStatusModel(
                    level = 1,
                    currentLevelBoosts = 2,
                    boosts = 3,
                    giftBoosts = 0,
                    nextLevelBoosts = 5,
                    premiumAudiencePart = 0.0,
                    premiumAudienceTotal = 0.0,
                    boostUrl = "https://t.me/boost",
                    hasMyBoost = false,
                    myBoostSlots = emptyList(),
                    isMaxLevel = false
                )
            )
        }

        override suspend fun getMyBoosts(): Result<MyBoostsModel> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository failure"))
            return Result.Success(
                MyBoostsModel(
                    slots = listOf(
                        BoostSlotModel(
                            slot = 1,
                            peerDialogId = null,
                            date = 1000,
                            expires = 2000,
                            cooldownUntilDate = 0
                        )
                    )
                )
            )
        }

        override suspend fun checkCanApplyBoost(dialogId: Long): Result<CanApplyBoostModel> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository failure"))
            return Result.Success(
                CanApplyBoostModel(
                    canApply = true,
                    empty = false,
                    replaceDialogId = 0L,
                    alreadyActive = false,
                    needSelector = false,
                    floodWait = 0,
                    slot = 1,
                    boostCount = 0,
                    isMaxLevel = false
                )
            )
        }

        override suspend fun applyBoost(dialogId: Long, slots: List<Int>): Result<MyBoostsModel> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository failure"))
            return Result.Success(
                MyBoostsModel(
                    slots = listOf(
                        BoostSlotModel(1, null, 1000, 2000, 0),
                        BoostSlotModel(2, dialogId, 1000, 2000, 0)
                    )
                )
            )
        }
    }
}
