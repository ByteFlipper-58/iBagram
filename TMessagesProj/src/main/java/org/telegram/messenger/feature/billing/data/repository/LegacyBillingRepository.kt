package org.telegram.messenger.feature.billing.data.repository

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BillingController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.billing.data.mapper.BillingMapper
import org.telegram.messenger.feature.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository
import kotlin.coroutines.resume

class LegacyBillingRepository(
    private val controllerProvider: () -> BillingController? = {
        try {
            BillingController.getInstance()
        } catch (_: Throwable) {
            null
        }
    }
) : BillingRepository {

    private val controller: BillingController?
        get() = controllerProvider()

    override fun observeBillingState(): Flow<BillingStateModel> = callbackFlow {
        val observer = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            if (id == NotificationCenter.billingProductDetailsUpdated ||
                id == NotificationCenter.billingConfirmPurchaseError
            ) {
                trySend(getBillingState())
            }
        }

        try {
            NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.billingProductDetailsUpdated)
            NotificationCenter.getGlobalInstance().addObserver(observer, NotificationCenter.billingConfirmPurchaseError)
        } catch (_: Throwable) {}

        // Emit initial state
        trySend(getBillingState())

        awaitClose {
            try {
                NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.billingProductDetailsUpdated)
                NotificationCenter.getGlobalInstance().removeObserver(observer, NotificationCenter.billingConfirmPurchaseError)
            } catch (_: Throwable) {}
        }
    }.flowOn(Dispatchers.Main)

    override fun getBillingState(): BillingStateModel {
        val ready = isReady()
        val invoiceMode = isInvoiceMode()
        val premiumProduct = getPremiumProduct()
        val lastTx = getLastPremiumTransaction()
        val lastToken = getLastPremiumToken()
        return BillingStateModel(
            isReady = ready,
            isInvoiceMode = invoiceMode,
            premiumProduct = premiumProduct,
            lastPremiumTransaction = lastTx,
            lastPremiumToken = lastToken
        )
    }

    override suspend fun startConnection(): Result<Boolean> = withContext(Dispatchers.Main) {
        val ctrl = controller ?: return@withContext Result.failure("Billing controller is unavailable")
        if (isReady()) {
            return@withContext Result.Success(true)
        }
        suspendCancellableCoroutine<Result<Boolean>> { continuation ->
            var resumed = false
            ctrl.whenSetuped {
                if (!resumed) {
                    resumed = true
                    continuation.resume(Result.Success(ctrl.isReady()))
                }
            }
            try {
                ctrl.startConnection()
            } catch (e: Exception) {
                if (!resumed) {
                    resumed = true
                    continuation.resume(Result.failure("Failed to start billing connection: ${e.message}"))
                }
            }
        }
    }

    override fun isReady(): Boolean = controller?.isReady() ?: false

    override fun isInvoiceMode(): Boolean = BillingController.billingClientEmpty

    override fun getPremiumProduct(): BillingProductModel? {
        val details = BillingController.PREMIUM_PRODUCT_DETAILS
        return BillingMapper.mapProductDetails(details)
    }

    override fun getLastPremiumTransaction(): String? = controller?.lastPremiumTransaction

    override fun getLastPremiumToken(): String? = controller?.lastPremiumToken

    override fun formatCurrency(amount: Long, currency: String, exp: Int, rounded: Boolean): String {
        return controller?.formatCurrency(amount, currency, exp, rounded) ?: "$amount $currency"
    }

    override fun getCurrencyExp(currency: String): Int {
        return controller?.getCurrencyExp(currency) ?: 0
    }

    override suspend fun queryPurchases(productType: BillingProductType): Result<List<BillingPurchaseModel>> = withContext(Dispatchers.Main) {
        val ctrl = controller ?: return@withContext Result.failure("Billing controller is unavailable")
        if (!isReady()) {
            return@withContext Result.failure("Billing client is not ready")
        }
        suspendCancellableCoroutine<Result<List<BillingPurchaseModel>>> { continuation ->
            val typeStr = BillingMapper.toProductTypeString(productType)
            ctrl.queryPurchases(typeStr) { billingResult: BillingResult, purchases: List<Purchase>? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val mapped = BillingMapper.mapPurchases(purchases)
                    continuation.resume(Result.Success(mapped))
                } else {
                    continuation.resume(
                        Result.failure("Query purchases failed: ${BillingController.getResponseCodeString(billingResult.responseCode)} (${billingResult.debugMessage})")
                    )
                }
            }
        }
    }

    override suspend fun manageSubscription(productId: String): Result<Boolean> = withContext(Dispatchers.Main) {
        val ctrl = controller ?: return@withContext Result.failure("Billing controller is unavailable")
        val ctx = ApplicationLoader.applicationContext
        if (ctx == null) {
            return@withContext Result.failure("Application context is null")
        }
        val opened = ctrl.startManageSubscription(ctx, productId)
        if (opened) {
            Result.Success(true)
        } else {
            Result.failure("Could not open Play Store subscriptions page for $productId")
        }
    }
}
