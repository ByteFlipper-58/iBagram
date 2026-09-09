package org.telegram.messenger.feature.billing.presentation

import org.telegram.messenger.feature.billing.domain.model.BillingProductModel
import org.telegram.messenger.feature.billing.domain.model.BillingPurchaseModel

data class BillingUiState(
    val isReady: Boolean = false,
    val isInvoiceMode: Boolean = false,
    val premiumProduct: BillingProductModel? = null,
    val activePurchases: List<BillingPurchaseModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
