package org.telegram.messenger.feature.billing.domain.model

data class BillingPriceModel(
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String
)
