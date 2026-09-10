package org.telegram.messenger.feature.business.billing.domain.model

data class BillingProductModel(
    val productId: String,
    val productType: BillingProductType,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val pricingPhases: List<BillingPriceModel> = emptyList()
)
