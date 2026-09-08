package org.telegram.messenger.feature.payments.domain.model

data class StarSubscriptionModel(
    val id: String,
    val peerId: Long,
    val untilDate: Long,
    val pricingAmount: Long,
    val isCanceled: Boolean = false,
    val inviteHash: String? = null
)
