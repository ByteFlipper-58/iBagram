package org.telegram.messenger.feature.giftauctions.domain.model

data class GiftAuctionAcquiredGiftModel(
    val giftId: Long,
    val num: Int,
    val date: Long,
    val peerId: Long,
    val bidAmount: Long = 0L,
    val round: Int = 0,
    val pos: Int = 0,
    val messageText: String? = null
)
