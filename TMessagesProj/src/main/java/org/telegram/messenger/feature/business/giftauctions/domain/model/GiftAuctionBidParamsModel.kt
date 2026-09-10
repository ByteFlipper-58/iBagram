package org.telegram.messenger.feature.business.giftauctions.domain.model

data class GiftAuctionBidParamsModel(
    val dialogId: Long = 0L,
    val message: String? = null,
    val hideName: Boolean = false
)
