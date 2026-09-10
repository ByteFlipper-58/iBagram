package org.telegram.messenger.feature.business.payments.presentation

import org.telegram.messenger.feature.business.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.business.payments.domain.model.StarsBalanceModel

data class PaymentsUiState(
    val balance: StarsBalanceModel = StarsBalanceModel(),
    val transactions: List<StarTransactionModel> = emptyList(),
    val subscriptions: List<StarSubscriptionModel> = emptyList(),
    val topupOptions: List<StarTopupOptionModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
