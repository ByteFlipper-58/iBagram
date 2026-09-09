package org.telegram.messenger.feature.giftauctions.domain.model

data class GiftAuctionModel(
    val giftId: Long,
    val title: String,
    val slug: String? = null,
    val status: GiftAuctionStatus = GiftAuctionStatus.UNKNOWN,
    val currentBid: Long = 0L,
    val minBid: Long = 0L,
    val userBid: Long = 0L,
    val userBidDate: Long = 0L,
    val endDate: Long = 0L,
    val acquiredCount: Int = 0,
    val previewAttributes: List<String> = emptyList(),
    val acquiredGiftsCount: Int = 0,
    val isFinished: Boolean = false
) {
    val hasUserBid: Boolean
        get() = userBid > 0L && userBidDate > 0L

    val isActive: Boolean
        get() = status == GiftAuctionStatus.ACTIVE && !isFinished
}
