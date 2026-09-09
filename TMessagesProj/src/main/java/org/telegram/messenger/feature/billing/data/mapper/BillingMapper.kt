package org.telegram.messenger.feature.billing.data.mapper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.telegram.messenger.feature.billing.domain.model.BillingPriceModel
import org.telegram.messenger.feature.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.billing.domain.model.BillingProductType
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseModel
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseState

object BillingMapper {

    fun mapProductType(typeString: String?): BillingProductType {
        return when (typeString) {
            BillingClient.ProductType.SUBS -> BillingProductType.SUBS
            else -> BillingProductType.INAPP
        }
    }

    fun toProductTypeString(type: BillingProductType): String {
        return when (type) {
            BillingProductType.SUBS -> BillingClient.ProductType.SUBS
            BillingProductType.INAPP -> BillingClient.ProductType.INAPP
        }
    }

    fun mapProductDetails(details: ProductDetails?): BillingProductModel? {
        if (details == null) return null

        val productType = mapProductType(details.productType)
        val phases = mutableListOf<BillingPriceModel>()
        var formattedPrice = ""
        var priceAmountMicros = 0L
        var priceCurrencyCode = ""

        if (productType == BillingProductType.SUBS) {
            val offers = details.subscriptionOfferDetails
            if (!offers.isNullOrEmpty()) {
                val firstOffer = offers[0]
                val pricingPhases = firstOffer.pricingPhases.pricingPhaseList
                for (phase in pricingPhases) {
                    phases.add(
                        BillingPriceModel(
                            formattedPrice = phase.formattedPrice,
                            priceAmountMicros = phase.priceAmountMicros,
                            priceCurrencyCode = phase.priceCurrencyCode
                        )
                    )
                }
                if (phases.isNotEmpty()) {
                    formattedPrice = phases[0].formattedPrice
                    priceAmountMicros = phases[0].priceAmountMicros
                    priceCurrencyCode = phases[0].priceCurrencyCode
                }
            }
        } else {
            val oneTime = details.oneTimePurchaseOfferDetails
            if (oneTime != null) {
                formattedPrice = oneTime.formattedPrice
                priceAmountMicros = oneTime.priceAmountMicros
                priceCurrencyCode = oneTime.priceCurrencyCode
                phases.add(
                    BillingPriceModel(
                        formattedPrice = formattedPrice,
                        priceAmountMicros = priceAmountMicros,
                        priceCurrencyCode = priceCurrencyCode
                    )
                )
            }
        }

        return BillingProductModel(
            productId = details.productId,
            productType = productType,
            title = details.title.orEmpty(),
            description = details.description.orEmpty(),
            formattedPrice = formattedPrice,
            priceAmountMicros = priceAmountMicros,
            priceCurrencyCode = priceCurrencyCode,
            pricingPhases = phases
        )
    }

    fun mapPurchaseState(state: Int): BillingPurchaseState {
        return when (state) {
            Purchase.PurchaseState.PURCHASED -> BillingPurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> BillingPurchaseState.PENDING
            else -> BillingPurchaseState.UNSPECIFIED
        }
    }

    fun mapPurchase(purchase: Purchase?): BillingPurchaseModel? {
        if (purchase == null) return null
        return BillingPurchaseModel(
            orderId = purchase.orderId.orEmpty(),
            purchaseToken = purchase.purchaseToken.orEmpty(),
            products = purchase.products ?: emptyList(),
            purchaseTime = purchase.purchaseTime,
            purchaseState = mapPurchaseState(purchase.purchaseState),
            isAcknowledged = purchase.isAcknowledged,
            isAutoRenewing = purchase.isAutoRenewing
        )
    }

    fun mapPurchases(purchases: List<Purchase>?): List<BillingPurchaseModel> {
        if (purchases.isNullOrEmpty()) return emptyList()
        return purchases.mapNotNull { mapPurchase(it) }
    }
}
