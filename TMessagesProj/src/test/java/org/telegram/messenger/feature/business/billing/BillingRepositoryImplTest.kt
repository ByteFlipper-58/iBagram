package org.telegram.messenger.feature.business.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.billing.data.datasource.BillingLocalDataSource
import org.telegram.messenger.feature.business.billing.data.datasource.BillingRemoteDataSource
import org.telegram.messenger.feature.business.billing.data.repository.BillingRepositoryImpl
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductType

@OptIn(ExperimentalCoroutinesApi::class)
class BillingRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: FakeBillingLocalDataSource
    private lateinit var remoteDataSource: BillingRemoteDataSource
    private lateinit var repository: BillingRepositoryImpl

    private class FakeBillingLocalDataSource(account: Int) : BillingLocalDataSource(account) {
        var readyState = false
        var invoiceModeState = false
        var premiumProductState: BillingProductModel? = null
        var lastTxState: String? = null
        var lastTokenState: String? = null
        var manageSubscriptionResult = true

        override fun isReady(): Boolean = readyState
        override fun isInvoiceMode(): Boolean = invoiceModeState
        override fun getPremiumProduct(): BillingProductModel? = premiumProductState
        override fun getLastPremiumTransaction(): String? = lastTxState
        override fun getLastPremiumToken(): String? = lastTokenState
        override fun formatCurrency(amount: Long, currency: String, exp: Int, rounded: Boolean): String = "$amount $currency"
        override fun getCurrencyExp(currency: String): Int = 2

        override fun startConnection(onSetupDone: (Boolean) -> Unit) {
            readyState = true
            onSetupDone(true)
        }

        override fun queryPurchases(productType: String, onResult: (BillingResult, List<Purchase>?) -> Unit) {
            val res = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .build()
            onResult(res, emptyList())
        }

        override fun manageSubscription(context: Context?, productId: String): Boolean {
            return manageSubscriptionResult
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = FakeBillingLocalDataSource(0)
        remoteDataSource = BillingRemoteDataSource(0)
        repository = BillingRepositoryImpl(
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
    fun testInitialBillingState() {
        val state = repository.getBillingState()
        assertNotNull(state)
        assertFalse(state.isReady)
        assertFalse(state.isInvoiceMode)
    }

    @Test
    fun testStartConnectionSuccess() = runTest {
        assertFalse(repository.isReady())
        val res = repository.startConnection()
        assertTrue(res is Result.Success)
        assertTrue(repository.isReady())
    }

    @Test
    fun testFormatCurrency() {
        val formatted = repository.formatCurrency(1000L, "USD")
        assertEquals("1000 USD", formatted)
        assertEquals(2, repository.getCurrencyExp("USD"))
    }

    @Test
    fun testQueryPurchases() = runTest {
        localDataSource.readyState = true
        val result = repository.queryPurchases(BillingProductType.SUBS)
        assertTrue(result is Result.Success)
        val purchases = (result as Result.Success).data
        assertTrue(purchases.isEmpty())
    }

    @Test
    fun testManageSubscription() = runTest {
        val res = repository.manageSubscription("telegram_premium")
        assertTrue(res is Result.Success)
    }

    @Test
    fun testObserveBillingState() = runTest {
        val state = repository.observeBillingState().first()
        assertNotNull(state)
    }
}
