package org.telegram.messenger.feature.business.billing.data.datasource

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BillingController
import org.telegram.messenger.feature.business.billing.data.mapper.BillingMapper
import org.telegram.messenger.feature.business.billing.domain.model.BillingProductModel

/**
 * Local data source for Google Play Billing interactions and cached product details.
 */
open class BillingLocalDataSource(
    private val currentAccount: Int
) {

    open fun getBillingController(): BillingController? {
        return try {
            BillingController.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    open fun isReady(): Boolean {
        return getBillingController()?.isReady ?: false
    }

    open fun isInvoiceMode(): Boolean {
        return BillingController.billingClientEmpty
    }

    open fun getPremiumProduct(): BillingProductModel? {
        val details = BillingController.PREMIUM_PRODUCT_DETAILS
        return BillingMapper.mapProductDetails(details)
    }

    open fun getLastPremiumTransaction(): String? {
        return getBillingController()?.lastPremiumTransaction
    }

    open fun getLastPremiumToken(): String? {
        return getBillingController()?.lastPremiumToken
    }

    open fun formatCurrency(amount: Long, currency: String, exp: Int = 0, rounded: Boolean = false): String {
        return getBillingController()?.formatCurrency(amount, currency, exp, rounded) ?: "$amount $currency"
    }

    open fun getCurrencyExp(currency: String): Int {
        return getBillingController()?.getCurrencyExp(currency) ?: 0
    }

    open fun startConnection(onSetupDone: (Boolean) -> Unit) {
        val controller = getBillingController()
        if (controller == null) {
            onSetupDone(false)
            return
        }
        if (controller.isReady) {
            onSetupDone(true)
            return
        }
        controller.whenSetuped {
            onSetupDone(controller.isReady)
        }
        try {
            controller.startConnection()
        } catch (_: Throwable) {
            onSetupDone(false)
        }
    }

    open fun queryPurchases(productType: String, onResult: (BillingResult, List<Purchase>?) -> Unit) {
        val controller = getBillingController()
        if (controller == null || !controller.isReady) {
            val result = BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("Billing controller unavailable")
                .build()
            onResult(result, null)
            return
        }
        controller.queryPurchases(productType) { res, purchases ->
            onResult(res, purchases)
        }
    }

    open fun manageSubscription(context: Context?, productId: String): Boolean {
        val controller = getBillingController() ?: return false
        val ctx = context ?: ApplicationLoader.applicationContext ?: return false
        return controller.startManageSubscription(ctx, productId)
    }
}
