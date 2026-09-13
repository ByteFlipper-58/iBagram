package org.telegram.messenger.feature.business.billing.data.repository

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineDispatcher
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
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.billing.data.datasource.BillingLocalDataSource
import org.telegram.messenger.feature.business.billing.data.datasource.BillingRemoteDataSource
import org.telegram.messenger.feature.business.billing.data.mapper.BillingMapper
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.business.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.business.billing.domain.model.BillingStateModel
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository
import kotlin.coroutines.resume

class BillingRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BillingLocalDataSource,
    private val remoteDataSource: BillingRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BillingRepository {

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
    }.flowOn(mainDispatcher)

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

    override suspend fun startConnection(): Result<Boolean> = withContext(mainDispatcher) {
        if (isReady()) {
            return@withContext Result.Success(true)
        }
        suspendCancellableCoroutine<Result<Boolean>> { continuation ->
            var resumed = false
            localDataSource.startConnection { isReady ->
                if (!resumed) {
                    resumed = true
                    if (isReady) {
                        continuation.resume(Result.Success(true))
                    } else {
                        continuation.resume(Result.Failure(AppError.Network("Failed to connect to billing service")))
                    }
                }
            }
        }
    }

    override fun isReady(): Boolean = localDataSource.isReady()

    override fun isInvoiceMode(): Boolean = localDataSource.isInvoiceMode()

    override fun getPremiumProduct(): BillingProductModel? = localDataSource.getPremiumProduct()

    override fun getLastPremiumTransaction(): String? = localDataSource.getLastPremiumTransaction()

    override fun getLastPremiumToken(): String? = localDataSource.getLastPremiumToken()

    override fun formatCurrency(amount: Long, currency: String, exp: Int, rounded: Boolean): String {
        return localDataSource.formatCurrency(amount, currency, exp, rounded)
    }

    override fun getCurrencyExp(currency: String): Int {
        return localDataSource.getCurrencyExp(currency)
    }

    override suspend fun queryPurchases(productType: BillingProductType): Result<List<BillingPurchaseModel>> =
        withContext(mainDispatcher) {
            if (!isReady()) {
                return@withContext Result.Failure(AppError.Network("Billing client is not ready"))
            }
            suspendCancellableCoroutine<Result<List<BillingPurchaseModel>>> { continuation ->
                val typeStr = BillingMapper.toProductTypeString(productType)
                localDataSource.queryPurchases(typeStr) { billingResult: BillingResult, purchases: List<Purchase>? ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val mapped = BillingMapper.mapPurchases(purchases)
                        continuation.resume(Result.Success(mapped))
                    } else {
                        continuation.resume(
                            Result.Failure(
                                AppError.Network("Query purchases failed: ${BillingController.getResponseCodeString(billingResult.responseCode)} (${billingResult.debugMessage})")
                            )
                        )
                    }
                }
            }
        }

    override suspend fun manageSubscription(productId: String): Result<Boolean> = withContext(mainDispatcher) {
        val opened = localDataSource.manageSubscription(null, productId)
        if (opened) {
            Result.Success(true)
        } else {
            Result.Failure(AppError.NotFound("Could not open Play Store subscriptions page for $productId"))
        }
    }
}
