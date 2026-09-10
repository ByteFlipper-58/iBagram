package org.telegram.messenger.feature.business.payments.domain.model

data class StarTransactionModel(
    val id: String,
    val amount: Long,
    val date: Long,
    val title: String? = null,
    val description: String? = null,
    val isRefund: Boolean = false,
    val isPending: Boolean = false,
    val isFailed: Boolean = false,
    val peerId: Long = 0L
)
