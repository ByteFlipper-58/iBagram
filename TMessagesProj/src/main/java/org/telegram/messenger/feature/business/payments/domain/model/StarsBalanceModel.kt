package org.telegram.messenger.feature.business.payments.domain.model

data class StarsBalanceModel(
    val balance: Long = 0L,
    val currency: String = "XTR",
    val isAvailable: Boolean = false
)
