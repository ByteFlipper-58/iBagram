package org.telegram.messenger.feature.social.boosts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.ChannelBoostsController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.data.datasource.BoostsLocalDataSource
import org.telegram.messenger.feature.social.boosts.data.datasource.BoostsRemoteDataSource
import org.telegram.messenger.feature.social.boosts.data.repository.BoostsRepositoryImpl
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stats
import org.telegram.tgnet.tl.TL_stories

@OptIn(ExperimentalCoroutinesApi::class)
class BoostsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeBoostsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeBoostsRemoteDataSource
    private lateinit var repository: BoostsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeBoostsLocalDataSource()
        fakeRemoteDataSource = FakeBoostsRemoteDataSource()
        repository = BoostsRepositoryImpl(
            currentAccount = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetBoostsStatusSuccess() = runTest {
        val status = TL_stories.TL_premium_boostsStatus().apply {
            level = 3
            current_level_boosts = 10
            boosts = 15
            gift_boosts = 2
            next_level_boosts = 20
            premium_audience = TL_stats.TL_statsPercentValue().apply {
                part = 10.0
                total = 100.0
            }
            boost_url = "https://t.me/boost/channel"
            my_boost = true
            my_boost_slots.add(1)
        }
        fakeRemoteDataSource.statusResponse = status

        val result = repository.getBoostsStatus(-1001234567890L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(3, data.level)
        assertEquals(10, data.currentLevelBoosts)
        assertEquals(15, data.boosts)
        assertEquals(2, data.giftBoosts)
        assertEquals(20, data.nextLevelBoosts)
        assertEquals(10.0, data.premiumAudiencePart, 0.001)
        assertTrue(data.hasMyBoost)
        assertEquals(listOf(1), data.myBoostSlots)
    }

    @Test
    fun testGetBoostsStatusMissingPeer() = runTest {
        fakeLocalDataSource.peerToReturn = null
        val result = repository.getBoostsStatus(999L)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.message.contains("Failed to resolve input peer"))
    }

    @Test
    fun testGetBoostsStatusFailure() = runTest {
        fakeRemoteDataSource.shouldFail = true
        val result = repository.getBoostsStatus(-1001234567890L)
        assertTrue(result is Result.Failure)
        assertEquals("Server error", (result as Result.Failure).error.message)
    }

    @Test
    fun testGetMyBoostsSuccessAndCache() = runTest {
        val boost = TL_stories.TL_myBoost().apply {
            slot = 1
            date = 1000
            expires = 2000
        }
        val user = TLRPC.TL_user().apply { id = 55L }
        val chat = TLRPC.TL_channel().apply { id = 66L }

        val myBoosts = TL_stories.TL_premium_myBoosts().apply {
            my_boosts.add(boost)
            users.add(user)
            chats.add(chat)
        }
        fakeRemoteDataSource.myBoostsResponse = myBoosts

        val result = repository.getMyBoosts()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.slots.size)
        assertEquals(1, data.slots[0].slot)
        assertEquals(1, fakeLocalDataSource.putUsersCalls)
    }

    @Test
    fun testCheckCanApplyBoostSuccess() = runTest {
        val canApplyBoost = ChannelBoostsController.CanApplyBoost().apply {
            canApply = true
            empty = false
            replaceDialogId = 0L
            slot = 2
            boostCount = 0
            isMaxLvl = false
        }
        fakeLocalDataSource.canApplyToReturn = canApplyBoost

        val result = repository.checkCanApplyBoost(-1001234567890L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.canApply)
        assertEquals(2, data.slot)
    }

    @Test
    fun testApplyBoostSuccess() = runTest {
        val boost = TL_stories.TL_myBoost().apply {
            slot = 2
            date = 1000
            expires = 2000
        }
        val myBoosts = TL_stories.TL_premium_myBoosts().apply {
            my_boosts.add(boost)
        }
        fakeRemoteDataSource.myBoostsResponse = myBoosts

        val result = repository.applyBoost(-1001234567890L, listOf(2))
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.slots.size)
        assertEquals(2, data.slots[0].slot)
        assertEquals(listOf(2), fakeRemoteDataSource.lastAppliedSlots)
    }

    @Test
    fun testStranglerHook() {
        val repo = ChannelBoostsController.getBoostsRepository(0)
        assertNotNull(repo)
    }

    private class FakeBoostsLocalDataSource : BoostsLocalDataSource(0) {
        var peerToReturn: TLRPC.InputPeer? = TLRPC.TL_inputPeerChannel().apply { channel_id = 1234567890L }
        var putUsersCalls = 0
        var canApplyToReturn: ChannelBoostsController.CanApplyBoost? = null

        override fun getInputPeer(dialogId: Long): TLRPC.InputPeer? = peerToReturn

        override fun putUsersAndChats(users: java.util.ArrayList<TLRPC.User>?, chats: java.util.ArrayList<TLRPC.Chat>?) {
            putUsersCalls++
        }

        override suspend fun checkCanApplyBoost(
            dialogId: Long,
            boostsStatus: TL_stories.TL_premium_boostsStatus
        ): ChannelBoostsController.CanApplyBoost? = canApplyToReturn
    }

    private class FakeBoostsRemoteDataSource : BoostsRemoteDataSource(0) {
        var shouldFail = false
        var statusResponse: TL_stories.TL_premium_boostsStatus = TL_stories.TL_premium_boostsStatus()
        var myBoostsResponse: TL_stories.TL_premium_myBoosts = TL_stories.TL_premium_myBoosts()
        var lastAppliedSlots: List<Int>? = null

        override suspend fun getBoostsStatus(peer: TLRPC.InputPeer): Result<TL_stories.TL_premium_boostsStatus> {
            if (shouldFail) return Result.failure("Server error")
            return Result.Success(statusResponse)
        }

        override suspend fun getMyBoosts(): Result<TL_stories.TL_premium_myBoosts> {
            if (shouldFail) return Result.failure("Server error")
            return Result.Success(myBoostsResponse)
        }

        override suspend fun applyBoost(peer: TLRPC.InputPeer, slots: List<Int>): Result<TL_stories.TL_premium_myBoosts> {
            lastAppliedSlots = slots
            if (shouldFail) return Result.failure("Server error")
            return Result.Success(myBoostsResponse)
        }
    }
}
