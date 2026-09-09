package org.telegram.messenger.feature.billing.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.billing.domain.model.BillingStateModel

interface BillingRepository {
    fun observeBillingState(): Flow<BillingStateModel>
    fun getBillingState(): BillingStateModel
    suspend fun startConnection(): Result<Boolean>
    fun isReady(): Boolean
    fun isInvoiceMode(): Boolean
    fun getPremiumProduct(): BillingProductModel?
    fun getLastPremiumTransaction(): String?
    fun getLastPremiumToken(): String?
    fun formatCurrency(amount: Long, currency: String, exp: Int = 0, rounded: Boolean = false): String
    fun getCurrencyExp(currency: String): Int
    suspend fun queryPurchases(productType: BillingProductType): Result<List<BillingPurchaseModel>>
    suspend fun manageSubscription(productId: String = "telegram_premium"): Result<Boolean>
}
