package org.telegram.messenger.feature.business.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.telegram.messenger.feature.business.billing.data.mapper.BillingMapper
import org.telegram.messenger.feature.business.billing.domain.model.BillingPriceModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.business.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingPurchaseState
import org.telegram.messenger.feature.business.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository
import org.telegram.messenger.feature.business.billing.domain.usecase.FormatCurrencyUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetCurrencyExpUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetPremiumProductUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ManageSubscriptionUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ObserveBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.QueryBillingPurchasesUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.StartBillingConnectionUseCase
import org.telegram.messenger.feature.business.billing.presentation.BillingEvent
import org.telegram.messenger.feature.business.billing.presentation.BillingViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BillingDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBillingRepository : BillingRepository {
        var ready = false
        var invoiceMode = false
        var productDetails: BillingProductModel? = null
        var lastTx: String? = null
        var lastToken: String? = null
        var shouldFailConnect = false
        var shouldFailQuery = false
        var shouldFailManage = false

        val purchasesList = mutableListOf<BillingPurchaseModel>()
        private val stateFlow = MutableStateFlow(
            BillingStateModel(
                isReady = false,
                isInvoiceMode = false,
                premiumProduct = null,
                lastPremiumTransaction = null,
                lastPremiumToken = null
            )
        )

        fun updateState(
            isReady: Boolean = ready,
            isInvoiceMode: Boolean = invoiceMode,
            product: BillingProductModel? = productDetails,
            tx: String? = lastTx,
            token: String? = lastToken
        ) {
            ready = isReady
            invoiceMode = isInvoiceMode
            productDetails = product
            lastTx = tx
            lastToken = token
            stateFlow.value = BillingStateModel(
                isReady = isReady,
                isInvoiceMode = isInvoiceMode,
                premiumProduct = product,
                lastPremiumTransaction = tx,
                lastPremiumToken = token
            )
        }

        override fun observeBillingState(): Flow<BillingStateModel> = stateFlow

        override fun getBillingState(): BillingStateModel = stateFlow.value

        override suspend fun startConnection(): Result<Boolean> {
            return if (shouldFailConnect) {
                Result.failure("Billing connection timeout")
            } else {
                ready = true
                updateState(isReady = true)
                Result.Success(true)
            }
        }

        override fun isReady(): Boolean = ready

        override fun isInvoiceMode(): Boolean = invoiceMode

        override fun getPremiumProduct(): BillingProductModel? = productDetails

        override fun getLastPremiumTransaction(): String? = lastTx

        override fun getLastPremiumToken(): String? = lastToken

        override fun formatCurrency(amount: Long, currency: String, exp: Int, rounded: Boolean): String {
            if (currency == "TON") return "TON " + (amount / 1_000_000_000.0)
            if (currency == "XTR") return "XTR $amount"
            return "$amount $currency"
        }

        override fun getCurrencyExp(currency: String): Int {
            return when (currency) {
                "USD", "EUR" -> 2
                "JPY" -> 0
                else -> 2
            }
        }

        override suspend fun queryPurchases(productType: BillingProductType): Result<List<BillingPurchaseModel>> {
            return if (shouldFailQuery) {
                Result.failure("Failed to query purchases")
            } else {
                Result.Success(purchasesList.toList())
            }
        }

        override suspend fun manageSubscription(productId: String): Result<Boolean> {
            return if (shouldFailManage) {
                Result.failure("No store application found")
            } else {
                Result.Success(true)
            }
        }
    }

    @Test
    fun testBillingMapperProductTypesAndStates() {
        assertEquals(BillingProductType.SUBS, BillingMapper.mapProductType(BillingClient.ProductType.SUBS))
        assertEquals(BillingProductType.INAPP, BillingMapper.mapProductType(BillingClient.ProductType.INAPP))
        assertEquals(BillingProductType.INAPP, BillingMapper.mapProductType("unknown"))
        assertEquals(BillingProductType.INAPP, BillingMapper.mapProductType(null))

        assertEquals(BillingClient.ProductType.SUBS, BillingMapper.toProductTypeString(BillingProductType.SUBS))
        assertEquals(BillingClient.ProductType.INAPP, BillingMapper.toProductTypeString(BillingProductType.INAPP))

        assertEquals(BillingPurchaseState.PURCHASED, BillingMapper.mapPurchaseState(Purchase.PurchaseState.PURCHASED))
        assertEquals(BillingPurchaseState.PENDING, BillingMapper.mapPurchaseState(Purchase.PurchaseState.PENDING))
        assertEquals(BillingPurchaseState.UNSPECIFIED, BillingMapper.mapPurchaseState(Purchase.PurchaseState.UNSPECIFIED_STATE))
        assertEquals(BillingPurchaseState.UNSPECIFIED, BillingMapper.mapPurchaseState(999))

        assertNull(BillingMapper.mapProductDetails(null))
        assertNull(BillingMapper.mapPurchase(null))
        assertTrue(BillingMapper.mapPurchases(null).isEmpty())
        assertTrue(BillingMapper.mapPurchases(emptyList()).isEmpty())
    }

    @Test
    fun testCurrencyFormattingAndExponent() {
        val repo = FakeBillingRepository()
        val formatUseCase = FormatCurrencyUseCase(repo)
        val expUseCase = GetCurrencyExpUseCase(repo)

        assertEquals("TON 1.5", formatUseCase(1_500_000_000L, "TON"))
        assertEquals("XTR 500", formatUseCase(500L, "XTR"))
        assertEquals("1000 USD", formatUseCase(1000L, "USD"))

        assertEquals(2, expUseCase("USD"))
        assertEquals(2, expUseCase("EUR"))
        assertEquals(0, expUseCase("JPY"))
    }

    @Test
    fun testBillingUseCases() = runTest {
        val repo = FakeBillingRepository()
        val dummyProduct = BillingProductModel(
            productId = "telegram_premium",
            productType = BillingProductType.SUBS,
            title = "Telegram Premium",
            description = "Subscription to Telegram Premium",
            formattedPrice = "$4.99",
            priceAmountMicros = 4990000L,
            priceCurrencyCode = "USD",
            pricingPhases = listOf(
                BillingPriceModel(
                    formattedPrice = "$4.99",
                    priceAmountMicros = 4990000L,
                    priceCurrencyCode = "USD"
                )
            )
        )

        repo.productDetails = dummyProduct
        repo.purchasesList.add(
            BillingPurchaseModel(
                orderId = "GPA.1234-5678",
                purchaseToken = "token_abc_123",
                products = listOf("telegram_premium"),
                purchaseTime = 1700000000000L,
                purchaseState = BillingPurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true
            )
        )

        val getStateUseCase = GetBillingStateUseCase(repo)
        val startConnUseCase = StartBillingConnectionUseCase(repo)
        val getProductUseCase = GetPremiumProductUseCase(repo)
        val queryPurchasesUseCase = QueryBillingPurchasesUseCase(repo)
        val manageSubUseCase = ManageSubscriptionUseCase(repo)

        assertEquals(dummyProduct, getProductUseCase())
        assertFalse(repo.isReady())

        val connResult = startConnUseCase()
        assertTrue(connResult is Result.Success && connResult.data)
        assertTrue(repo.isReady())

        val purchasesResult = queryPurchasesUseCase(BillingProductType.SUBS)
        assertTrue(purchasesResult is Result.Success)
        val purchases = (purchasesResult as Result.Success).data
        assertEquals(1, purchases.size)
        assertEquals("GPA.1234-5678", purchases[0].orderId)
        assertEquals(BillingPurchaseState.PURCHASED, purchases[0].purchaseState)

        val manageResult = manageSubUseCase("telegram_premium")
        assertTrue(manageResult is Result.Success && manageResult.data)

        // Test failure branch
        repo.shouldFailQuery = true
        val failedQuery = queryPurchasesUseCase(BillingProductType.SUBS)
        assertTrue(failedQuery is Result.Failure)

        repo.shouldFailManage = true
        val failedManage = manageSubUseCase("telegram_premium")
        assertTrue(failedManage is Result.Failure)
    }

    @Test
    fun testBillingViewModelFlow() = runTest {
        val repo = FakeBillingRepository()
        val dummyProduct = BillingProductModel(
            productId = "telegram_premium",
            productType = BillingProductType.SUBS,
            title = "Telegram Premium",
            description = "Subscription to Telegram Premium",
            formattedPrice = "$4.99",
            priceAmountMicros = 4990000L,
            priceCurrencyCode = "USD"
        )
        repo.updateState(product = dummyProduct)

        val vm = BillingViewModel(
            observeBillingStateUseCase = ObserveBillingStateUseCase(repo),
            getBillingStateUseCase = GetBillingStateUseCase(repo),
            startBillingConnectionUseCase = StartBillingConnectionUseCase(repo),
            queryBillingPurchasesUseCase = QueryBillingPurchasesUseCase(repo),
            manageSubscriptionUseCase = ManageSubscriptionUseCase(repo)
        )

        advanceUntilIdle()
        val initialState = vm.uiState.value
        assertFalse(initialState.isReady)
        assertFalse(initialState.isInvoiceMode)
        assertEquals(dummyProduct, initialState.premiumProduct)

        // Trigger Connect
        vm.onEvent(BillingEvent.Connect)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isReady)
        assertFalse(vm.uiState.value.isLoading)

        // Query purchases
        repo.purchasesList.add(
            BillingPurchaseModel(
                orderId = "ORD_001",
                purchaseToken = "tok_001",
                products = listOf("telegram_premium"),
                purchaseTime = 1700000000L,
                purchaseState = BillingPurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true
            )
        )
        vm.onEvent(BillingEvent.QueryPurchases(BillingProductType.SUBS))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.activePurchases.size)
        assertEquals("ORD_001", vm.uiState.value.activePurchases[0].orderId)

        // Failure branch for query
        repo.shouldFailQuery = true
        vm.onEvent(BillingEvent.QueryPurchases(BillingProductType.SUBS))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        // Clear error
        vm.onEvent(BillingEvent.ClearError)
        assertNull(vm.uiState.value.errorMessage)

        // Manage subscription error
        repo.shouldFailManage = true
        vm.onEvent(BillingEvent.ManageSubscription("telegram_premium"))
        advanceUntilIdle()
        assertEquals("No store application found", vm.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.billingRepository)
        assertNotNull(container.observeBillingStateUseCase)
        assertNotNull(container.getBillingStateUseCase)
        assertNotNull(container.startBillingConnectionUseCase)
        assertNotNull(container.getPremiumProductUseCase)
        assertNotNull(container.formatCurrencyUseCase)
        assertNotNull(container.getCurrencyExpUseCase)
        assertNotNull(container.queryBillingPurchasesUseCase)
        assertNotNull(container.manageSubscriptionUseCase)

        val fakeRepo = FakeBillingRepository()
        container.billingRepository = fakeRepo
        assertEquals(fakeRepo, container.billingRepository)
        assertEquals(fakeRepo.getBillingState(), container.getBillingStateUseCase())

        val vm = container.createBillingViewModel()
        assertNotNull(vm)
        assertFalse(vm.uiState.value.isReady)
    }
}
