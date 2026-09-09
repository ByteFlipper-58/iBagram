package org.telegram.messenger.feature.giftauctions.presentation

import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionBidParamsModel

sealed class GiftAuctionsEvent {
    object LoadActiveAuctions : GiftAuctionsEvent()
    object RefreshActiveAuctions : GiftAuctionsEvent()
    data class SelectAuction(val giftId: Long) : GiftAuctionsEvent()
    data class LoadAuctionBySlug(val slug: String) : GiftAuctionsEvent()
    data class SendBid(
        val giftId: Long,
        val amount: Long,
        val params: GiftAuctionBidParamsModel? = null
    ) : GiftAuctionsEvent()
    data class LoadAcquiredGifts(val giftId: Long) : GiftAuctionsEvent()
    object ClearError : GiftAuctionsEvent()
}
