package org.telegram.messenger.feature.business.billing.presentation

import org.telegram.messenger.feature.business.billing.domain.model.BillingProductType

sealed interface BillingEvent {
    object Connect : BillingEvent
    data class QueryPurchases(val productType: BillingProductType) : BillingEvent
    data class ManageSubscription(val productId: String = "telegram_premium") : BillingEvent
    object ClearError : BillingEvent
}
