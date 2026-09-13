package org.telegram.messenger.feature.business.payments

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.data.datasource.PaymentsLocalDataSource
import org.telegram.messenger.feature.business.payments.data.datasource.PaymentsRemoteDataSource
import org.telegram.messenger.feature.business.payments.data.repository.PaymentsRepositoryImpl
import org.telegram.tgnet.tl.TL_stars

class PaymentsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: PaymentsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakePaymentsRemoteDataSource
    private lateinit var repository: PaymentsRepositoryImpl

    private class FakePaymentsRemoteDataSource(account: Int) : PaymentsRemoteDataSource(account) {
        var remoteBalance: Pair<Long, Boolean> = Pair(1000L, true)
        var remoteTransactions: List<TL_stars.StarsTransaction> = emptyList()
        var remoteSubscriptions: List<TL_stars.StarsSubscription> = emptyList()
        var remoteTopupOptions: List<TL_stars.TL_starsTopupOption> = emptyList()
        var shouldFail = false

        override suspend fun fetchBalance(): Result<Pair<Long, Boolean>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteBalance)
        }

        override suspend fun fetchTransactions(type: Int): Result<List<TL_stars.StarsTransaction>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteTransactions)
        }

        override suspend fun fetchSubscriptions(): Result<List<TL_stars.StarsSubscription>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteSubscriptions)
        }

        override suspend fun fetchTopupOptions(): Result<List<TL_stars.TL_starsTopupOption>> {
            if (shouldFail) return Result.failure("Remote failure")
            return Result.Success(remoteTopupOptions)
        }

        override suspend fun refreshBalance(): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshTransactions(): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshSubscriptions(): Result<Unit> = Result.Success(Unit)
    }

    @Before
    fun setUp() {
        localDataSource = PaymentsLocalDataSource(0).apply {
            setTestMode(true)
        }
        fakeRemoteDataSource = FakePaymentsRemoteDataSource(0)
        repository = PaymentsRepositoryImpl(0, localDataSource, fakeRemoteDataSource, testDispatcher)
    }

    @Test
    fun testGetBalanceFromCache() = runTest(testDispatcher) {
        localDataSource.setTestBalance(2500L, true)

        val result = repository.getBalance()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2500L, data.balance)
        assertEquals("XTR", data.currency)
        assertTrue(data.isAvailable)
    }

    @Test
    fun testGetBalanceFromRemoteWhenNotAvailable() = runTest(testDispatcher) {
        fakeRemoteDataSource.remoteBalance = Pair(5000L, true)

        val result = repository.getBalance()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(5000L, data.balance)
        assertTrue(data.isAvailable)
    }

    @Test
    fun testGetTransactions() = runTest(testDispatcher) {
        val tx = TL_stars.TL_starsTransaction().apply {
            this.id = "tx_pay_1"
            this.amount = TL_stars.StarsAmount.ofStars(150L)
            this.date = 1600000000
            this.title = "Payment 1"
            this.description = "Test Desc"
            this.refund = false
            this.pending = false
            this.failed = false
        }
        localDataSource.setTestTransactions(0, listOf(tx))

        val result = repository.getTransactions(0)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("tx_pay_1", list[0].id)
        assertEquals(150L, list[0].amount)
        assertEquals("Payment 1", list[0].title)
    }

    @Test
    fun testGetSubscriptions() = runTest(testDispatcher) {
        val sub = TL_stars.TL_starsSubscription().apply {
            this.id = "sub_1"
            this.until_date = 1800000000
            this.pricing = TL_stars.TL_starsSubscriptionPricing().apply {
                this.amount = 300L
            }
            this.canceled = false
            this.chat_invite_hash = "invite_xyz"
        }
        fakeRemoteDataSource.remoteSubscriptions = listOf(sub)

        val result = repository.getSubscriptions()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("sub_1", list[0].id)
        assertEquals(300L, list[0].pricingAmount)
        assertEquals("invite_xyz", list[0].inviteHash)
    }

    @Test
    fun testGetTopupOptions() = runTest(testDispatcher) {
        val opt = TL_stars.TL_starsTopupOption().apply {
            this.stars = 100L
            this.amount = 199L
            this.currency = "USD"
        }
        fakeRemoteDataSource.remoteTopupOptions = listOf(opt)

        val result = repository.getTopupOptions()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(100L, list[0].stars)
        assertEquals(199L, list[0].amount)
        assertEquals("USD", list[0].currency)
    }

    @Test
    fun testRefreshMethods() = runTest(testDispatcher) {
        assertTrue(repository.refreshBalance() is Result.Success)
        assertTrue(repository.refreshTransactions() is Result.Success)
        assertTrue(repository.refreshSubscriptions() is Result.Success)
    }

    @Test
    fun testObserveBalance() = runTest(testDispatcher) {
        localDataSource.setTestBalance(777L, true)

        val balance = repository.observeBalance().first()
        assertEquals(777L, balance.balance)
        assertTrue(balance.isAvailable)
    }
}
