package org.telegram.messenger.feature.billing.domain.model

data class BillingStateModel(
    val isReady: Boolean,
    val isInvoiceMode: Boolean,
    val premiumProduct: BillingProductModel?,
    val lastPremiumTransaction: String?,
    val lastPremiumToken: String?
)
