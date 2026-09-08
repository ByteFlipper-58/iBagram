package org.telegram.messenger.feature.payments.domain.model

data class StarTopupOptionModel(
    val stars: Long,
    val amount: Long,
    val currency: String
)
