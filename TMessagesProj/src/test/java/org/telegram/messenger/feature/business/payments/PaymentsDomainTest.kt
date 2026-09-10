package org.telegram.messenger.feature.business.payments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.payments.data.mapper.PaymentMapper
import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarsBalanceModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTopupOptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.presentation.PaymentsEvent
import org.telegram.messenger.feature.business.payments.presentation.PaymentsViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stars

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsDomainTest {

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
        val balance = StarsBalanceModel(
            balance = 500L,
            currency = "XTR",
            isAvailable = true
        )
        assertEquals(500L, balance.balance)
        assertEquals("XTR", balance.currency)
        assertTrue(balance.isAvailable)

        val transaction = StarTransactionModel(
            id = "tx_12345",
            amount = 150L,
            date = 1700000000L,
            title = "Test Payment",
            description = "Payment for stars",
            isRefund = false,
            isPending = false,
            isFailed = false,
            peerId = 987654L
        )
        assertEquals("tx_12345", transaction.id)
        assertEquals(150L, transaction.amount)
        assertEquals(1700000000L, transaction.date)
        assertEquals("Test Payment", transaction.title)
        assertEquals("Payment for stars", transaction.description)
        assertFalse(transaction.isRefund)
        assertFalse(transaction.isPending)
        assertFalse(transaction.isFailed)
        assertEquals(987654L, transaction.peerId)

        val subscription = StarSubscriptionModel(
            id = "sub_987",
            peerId = 123456L,
            untilDate = 1710000000L,
            pricingAmount = 50L,
            isCanceled = false,
            inviteHash = "hash123"
        )
        assertEquals("sub_987", subscription.id)
        assertEquals(123456L, subscription.peerId)
        assertEquals(1710000000L, subscription.untilDate)
        assertEquals(50L, subscription.pricingAmount)
        assertFalse(subscription.isCanceled)
        assertEquals("hash123", subscription.inviteHash)

        val topupOption = StarTopupOptionModel(
            stars = 250L,
            amount = 499L,
            currency = "USD"
        )
        assertEquals(250L, topupOption.stars)
        assertEquals(499L, topupOption.amount)
        assertEquals("USD", topupOption.currency)
    }

    @Test
    fun testPaymentMapper() {
        val mappedBalance = PaymentMapper.mapBalance(1000L, true)
        assertEquals(1000L, mappedBalance.balance)
        assertEquals("XTR", mappedBalance.currency)
        assertTrue(mappedBalance.isAvailable)

        val tx = TL_stars.TL_starsTransaction().apply {
            id = "tx_mapped_1"
            amount = TL_stars.TL_starsAmount().apply { amount = 300L }
            date = 1690000000
            title = "App Store Purchase"
            description = "Stars Topup"
            refund = false
            pending = false
            failed = false
            peer = TL_stars.TL_starsTransactionPeer().apply {
                peer = TLRPC.TL_peerUser().apply { user_id = 777L }
            }
        }
        val mappedTx = PaymentMapper.mapTransaction(tx)
        assertEquals("tx_mapped_1", mappedTx.id)
        assertEquals(300L, mappedTx.amount)
        assertEquals(1690000000L, mappedTx.date)
        assertEquals("App Store Purchase", mappedTx.title)
        assertEquals("Stars Topup", mappedTx.description)
        assertFalse(mappedTx.isRefund)
        assertEquals(777L, mappedTx.peerId)

        val sub = TL_stars.TL_starsSubscription().apply {
            id = "sub_mapped_1"
            until_date = 1720000000
            canceled = false
            chat_invite_hash = "invite_hash_abc"
            peer = TLRPC.TL_peerChannel().apply { channel_id = 888L }
            pricing = TL_stars.TL_starsSubscriptionPricing().apply {
                amount = 100L
            }
        }
        val mappedSub = PaymentMapper.mapSubscription(sub)
        assertEquals("sub_mapped_1", mappedSub.id)
        assertEquals(-888L, mappedSub.peerId)
        assertEquals(1720000000L, mappedSub.untilDate)
        assertEquals(100L, mappedSub.pricingAmount)
        assertFalse(mappedSub.isCanceled)
        assertEquals("invite_hash_abc", mappedSub.inviteHash)

        val opt = TL_stars.TL_starsTopupOption().apply {
            stars = 500L
            amount = 999L
            currency = "EUR"
        }
        val mappedOpt = PaymentMapper.mapTopupOption(opt)
        assertEquals(500L, mappedOpt.stars)
        assertEquals(999L, mappedOpt.amount)
        assertEquals("EUR", mappedOpt.currency)
    }

    @Test
    fun testUseCasesAndRepository() = runTest {
        val fakeRepo = FakePaymentsRepository()

        val observeBalanceUseCase = ObserveStarsBalanceUseCase(fakeRepo)
        val observeTransactionsUseCase = ObserveStarTransactionsUseCase(fakeRepo)
        val observeSubscriptionsUseCase = ObserveStarSubscriptionsUseCase(fakeRepo)
        val getBalanceUseCase = GetStarsBalanceUseCase(fakeRepo)
        val getTransactionsUseCase = GetStarTransactionsUseCase(fakeRepo)
        val getSubscriptionsUseCase = GetStarSubscriptionsUseCase(fakeRepo)
        val getTopupOptionsUseCase = GetStarTopupOptionsUseCase(fakeRepo)
        val refreshBalanceUseCase = RefreshStarsBalanceUseCase(fakeRepo)
        val refreshTransactionsUseCase = RefreshStarTransactionsUseCase(fakeRepo)
        val refreshSubscriptionsUseCase = RefreshStarSubscriptionsUseCase(fakeRepo)

        val balance = observeBalanceUseCase().first()
        assertEquals(100L, balance.balance)
        assertTrue(balance.isAvailable)

        val transactions = observeTransactionsUseCase().first()
        assertEquals(1, transactions.size)
        assertEquals("tx_fake_1", transactions[0].id)

        val subscriptions = observeSubscriptionsUseCase().first()
        assertEquals(1, subscriptions.size)
        assertEquals("sub_fake_1", subscriptions[0].id)

        val balanceResult = getBalanceUseCase()
        assertTrue(balanceResult is Result.Success)
        assertEquals(100L, (balanceResult as Result.Success).data.balance)

        val txResult = getTransactionsUseCase()
        assertTrue(txResult is Result.Success)
        assertEquals(1, (txResult as Result.Success).data.size)

        val subResult = getSubscriptionsUseCase()
        assertTrue(subResult is Result.Success)
        assertEquals(1, (subResult as Result.Success).data.size)

        val optionsResult = getTopupOptionsUseCase()
        assertTrue(optionsResult is Result.Success)
        assertEquals(1, (optionsResult as Result.Success).data.size)
        assertEquals(50L, (optionsResult as Result.Success).data[0].stars)

        val refBalResult = refreshBalanceUseCase()
        assertTrue(refBalResult is Result.Success)
        assertTrue(fakeRepo.balanceRefreshed)

        val refTxResult = refreshTransactionsUseCase()
        assertTrue(refTxResult is Result.Success)
        assertTrue(fakeRepo.transactionsRefreshed)

        val refSubResult = refreshSubscriptionsUseCase()
        assertTrue(refSubResult is Result.Success)
        assertTrue(fakeRepo.subscriptionsRefreshed)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        val fakeRepo = FakePaymentsRepository()
        val viewModel = PaymentsViewModel(
            observeStarsBalanceUseCase = ObserveStarsBalanceUseCase(fakeRepo),
            observeStarTransactionsUseCase = ObserveStarTransactionsUseCase(fakeRepo),
            observeStarSubscriptionsUseCase = ObserveStarSubscriptionsUseCase(fakeRepo),
            getStarTopupOptionsUseCase = GetStarTopupOptionsUseCase(fakeRepo),
            refreshStarsBalanceUseCase = RefreshStarsBalanceUseCase(fakeRepo),
            refreshStarTransactionsUseCase = RefreshStarTransactionsUseCase(fakeRepo),
            refreshStarSubscriptionsUseCase = RefreshStarSubscriptionsUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(100L, state.balance.balance)
        assertEquals(1, state.transactions.size)
        assertEquals(1, state.subscriptions.size)
        assertEquals(1, state.topupOptions.size)

        viewModel.onEvent(PaymentsEvent.RefreshBalance)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.balanceRefreshed)

        viewModel.onEvent(PaymentsEvent.RefreshTransactions)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.transactionsRefreshed)

        viewModel.onEvent(PaymentsEvent.RefreshSubscriptions)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.subscriptionsRefreshed)

        viewModel.onEvent(PaymentsEvent.RefreshAll)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakePaymentsRepository : PaymentsRepository {
        val sampleBalance = StarsBalanceModel(balance = 100L, currency = "XTR", isAvailable = true)
        val sampleTx = StarTransactionModel(id = "tx_fake_1", amount = 10L, date = 1700000000L)
        val sampleSub = StarSubscriptionModel(id = "sub_fake_1", peerId = 123L, untilDate = 1710000000L, pricingAmount = 10L)
        val sampleOption = StarTopupOptionModel(stars = 50L, amount = 99L, currency = "USD")

        val balanceFlow = MutableStateFlow(sampleBalance)
        val transactionsFlow = MutableStateFlow(listOf(sampleTx))
        val subscriptionsFlow = MutableStateFlow(listOf(sampleSub))

        var balanceRefreshed = false
        var transactionsRefreshed = false
        var subscriptionsRefreshed = false

        override fun observeBalance(): Flow<StarsBalanceModel> = balanceFlow
        override fun observeTransactions(): Flow<List<StarTransactionModel>> = transactionsFlow
        override fun observeSubscriptions(): Flow<List<StarSubscriptionModel>> = subscriptionsFlow

        override suspend fun getBalance(): Result<StarsBalanceModel> = Result.Success(sampleBalance)
        override suspend fun getTransactions(type: Int): Result<List<StarTransactionModel>> = Result.Success(listOf(sampleTx))
        override suspend fun getSubscriptions(): Result<List<StarSubscriptionModel>> = Result.Success(listOf(sampleSub))
        override suspend fun getTopupOptions(): Result<List<StarTopupOptionModel>> = Result.Success(listOf(sampleOption))

        override suspend fun refreshBalance(): Result<Unit> {
            balanceRefreshed = true
            return Result.Success(Unit)
        }

        override suspend fun refreshTransactions(): Result<Unit> {
            transactionsRefreshed = true
            return Result.Success(Unit)
        }

        override suspend fun refreshSubscriptions(): Result<Unit> {
            subscriptionsRefreshed = true
            return Result.Success(Unit)
        }
    }
}
