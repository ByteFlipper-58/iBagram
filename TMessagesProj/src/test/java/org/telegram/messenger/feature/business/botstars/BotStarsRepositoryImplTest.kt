package org.telegram.messenger.feature.business.botstars

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.data.datasource.BotStarsLocalDataSource
import org.telegram.messenger.feature.business.botstars.data.datasource.BotStarsRemoteDataSource
import org.telegram.messenger.feature.business.botstars.data.repository.BotStarsRepositoryImpl
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars

class BotStarsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: BotStarsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeBotStarsRemoteDataSource
    private lateinit var repository: BotStarsRepositoryImpl

    private class FakeBotStarsRemoteDataSource(account: Int) : BotStarsRemoteDataSource(account) {
        var remoteStarsStats: TLRPC.TL_payments_starsRevenueStats? = null
        var remoteTonStats: TLRPC.TL_payments_starsRevenueStats? = null
        var remoteTransactions: List<TL_stars.StarsTransaction> = emptyList()
        var remoteConnectedBots: List<TL_payments.connectedBotStarRef> = emptyList()
        var remoteSuggestedBots: List<TL_payments.starRefProgram> = emptyList()
        var remoteAdminedBots: List<Long> = emptyList()
        var remoteAdminedChannels: List<Long> = emptyList()
        var shouldFail = false

        override suspend fun fetchBotStarsStats(dialogId: Long, force: Boolean): Result<TLRPC.TL_payments_starsRevenueStats?> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteStarsStats)
        }

        override suspend fun fetchTonStats(dialogId: Long, force: Boolean): Result<TLRPC.TL_payments_starsRevenueStats?> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteTonStats)
        }

        override suspend fun fetchTransactions(dialogId: Long, type: Int, reload: Boolean): Result<List<TL_stars.StarsTransaction>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteTransactions)
        }

        override suspend fun fetchConnectedBots(dialogId: Long, reload: Boolean): Result<List<TL_payments.connectedBotStarRef>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteConnectedBots)
        }

        override suspend fun fetchSuggestedBots(dialogId: Long, sort: Int): Result<List<TL_payments.starRefProgram>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteSuggestedBots)
        }

        override suspend fun fetchAdminedBots(): Result<List<Long>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteAdminedBots)
        }

        override suspend fun fetchAdminedChannels(): Result<List<Long>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteAdminedChannels)
        }
    }

    @Before
    fun setUp() {
        localDataSource = BotStarsLocalDataSource(0).apply {
            setTestMode(true)
        }
        fakeRemoteDataSource = FakeBotStarsRemoteDataSource(0)
        repository = BotStarsRepositoryImpl(0, localDataSource, fakeRemoteDataSource, testDispatcher)
    }

    private fun createRevenueStats(currentBalance: Long, availableBalance: Long): TLRPC.TL_payments_starsRevenueStats {
        val stats = TLRPC.TL_payments_starsRevenueStats()
        stats.status = TLRPC.TL_starsRevenueStatus().apply {
            this.current_balance = TL_stars.StarsAmount.ofStars(currentBalance)
            this.available_balance = TL_stars.StarsAmount.ofStars(availableBalance)
            this.overall_revenue = TL_stars.StarsAmount.ofStars(currentBalance + availableBalance)
            this.withdrawal_enabled = true
            this.next_withdrawal_at = 12345
        }
        stats.usd_rate = 1.5
        return stats
    }

    @Test
    fun testGetBotStarsStatsFromCache() = runTest(testDispatcher) {
        val stats = createRevenueStats(1000L, 500L)
        localDataSource.setTestStarsRevenueStats(123L, stats)

        val result = repository.getBotStarsStats(123L, force = false)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data)
        assertEquals(123L, data?.dialogId)
        assertEquals(1000L, data?.status?.currentBalance)
        assertEquals(500L, data?.status?.availableBalance)
    }

    @Test
    fun testGetBotStarsStatsFromRemoteWhenCacheEmpty() = runTest(testDispatcher) {
        val stats = createRevenueStats(2000L, 800L)
        fakeRemoteDataSource.remoteStarsStats = stats

        val result = repository.getBotStarsStats(456L, force = false)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data)
        assertEquals(456L, data?.dialogId)
        assertEquals(2000L, data?.status?.currentBalance)
        assertEquals(800L, data?.status?.availableBalance)
    }

    @Test
    fun testGetTonStats() = runTest(testDispatcher) {
        val stats = createRevenueStats(5000L, 1000L)
        localDataSource.setTestTonRevenueStats(789L, stats)

        val result = repository.getTonStats(789L, force = false)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data)
        assertEquals(789L, data?.dialogId)
        assertEquals(5000L, data?.status?.currentBalance)
    }

    @Test
    fun testLoadTransactions() = runTest(testDispatcher) {
        val tx = TL_stars.TL_starsTransaction().apply {
            this.id = "tx_1"
            this.amount = TL_stars.StarsAmount.ofStars(250L)
            this.date = 1000
            this.title = "Test TX"
            this.description = "TX description"
            this.flags = 0
        }
        localDataSource.setTestTransactions(100L, 0, listOf(tx))

        val result = repository.loadTransactions(100L, BotStarsTransactionType.ALL, reload = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("tx_1", list[0].id)
        assertEquals(250L, list[0].stars)
        assertEquals("Test TX", list[0].title)
    }

    @Test
    fun testLoadConnectedBots() = runTest(testDispatcher) {
        val bot = TL_payments.connectedBotStarRef().apply {
            this.bot_id = 999L
            this.date = 2000
            this.url = "https://t.me/testbot"
            this.commission_permille = 50
            this.duration_months = 3
            this.revoked = false
            this.participants = 10
            this.revenue = 500L
        }
        localDataSource.setTestConnectedBots(200L, listOf(bot))

        val result = repository.loadConnectedBots(200L, reload = false)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(999L, list[0].botId)
        assertEquals("https://t.me/testbot", list[0].url)
        assertEquals(50, list[0].commissionPermille)
    }

    @Test
    fun testLoadSuggestedBots() = runTest(testDispatcher) {
        val suggested = TL_payments.starRefProgram().apply {
            this.bot_id = 888L
            this.commission_permille = 100
            this.duration_months = 6
            this.end_date = 3000
            this.daily_revenue_per_user = TL_stars.StarsAmount.ofStars(15L)
        }
        fakeRemoteDataSource.remoteSuggestedBots = listOf(suggested)

        val result = repository.loadSuggestedBots(300L, sort = 0)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(888L, list[0].botId)
        assertEquals(100, list[0].commissionPermille)
        assertEquals(15L, list[0].dailyRevenuePerUser)
    }

    @Test
    fun testLoadAdminedBotsAndChannels() = runTest(testDispatcher) {
        localDataSource.setTestAdminedBots(listOf(101L, 102L))
        localDataSource.setTestAdminedChannels(listOf(201L, 202L, 203L))

        val botsResult = repository.loadAdminedBots()
        assertTrue(botsResult is Result.Success)
        assertEquals(listOf(101L, 102L), (botsResult as Result.Success).data)

        val channelsResult = repository.loadAdminedChannels()
        assertTrue(channelsResult is Result.Success)
        assertEquals(listOf(201L, 202L, 203L), (channelsResult as Result.Success).data)
    }

    @Test
    fun testObserveBotStarsStats() = runTest(testDispatcher) {
        val stats = createRevenueStats(777L, 333L)
        localDataSource.setTestStarsRevenueStats(555L, stats)

        val emitted = repository.observeBotStarsStats(555L).first()
        assertNotNull(emitted)
        assertEquals(555L, emitted?.dialogId)
        assertEquals(777L, emitted?.status?.currentBalance)
    }
}
