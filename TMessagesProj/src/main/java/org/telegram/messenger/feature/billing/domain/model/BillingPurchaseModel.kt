package org.telegram.messenger.feature.billing.domain.model

data class BillingPurchaseModel(
    val orderId: String,
    val purchaseToken: String,
    val products: List<String>,
    val purchaseTime: Long,
    val purchaseState: BillingPurchaseState,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean
)
