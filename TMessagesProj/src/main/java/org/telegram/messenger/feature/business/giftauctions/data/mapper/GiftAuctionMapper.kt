package org.telegram.messenger.feature.business.giftauctions.data.mapper

import org.telegram.messenger.DialogObject
import org.telegram.messenger.GiftAuctionController
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionStatus
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stars
import org.telegram.ui.Gifts.AuctionBidSheet

object GiftAuctionMapper {

    fun mapAuction(auction: GiftAuctionController.Auction?): GiftAuctionModel? {
        if (auction == null) return null

        val status = when {
            auction.isFinished -> GiftAuctionStatus.FINISHED
            auction.auctionState is TL_stars.TL_starGiftAuctionState -> GiftAuctionStatus.ACTIVE
            auction.auctionState is TL_stars.TL_starGiftAuctionStateFinished -> GiftAuctionStatus.FINISHED
            else -> GiftAuctionStatus.UNKNOWN
        }

        val endDate = when (val s = auction.auctionState) {
            is TL_stars.TL_starGiftAuctionState -> s.end_date.toLong()
            else -> 0L
        }

        val previewAttrs = auction.previewAttributes?.mapNotNull { attr ->
            when (attr) {
                is TL_stars.starGiftAttributeModel -> attr.name
                is TL_stars.starGiftAttributePattern -> attr.name
                is TL_stars.starGiftAttributeBackdrop -> attr.name
                else -> null
            }
        } ?: emptyList()

        val title = auction.gift.title
            ?: auction.giftAuctionSlug
            ?: "Gift #${auction.giftId}"

        return GiftAuctionModel(
            giftId = auction.giftId,
            title = title,
            slug = auction.giftAuctionSlug,
            status = status,
            currentBid = auction.currentTopBid,
            minBid = auction.minimumBid,
            userBid = auction.currentMyBid,
            userBidDate = auction.auctionUserState.bid_date.toLong(),
            endDate = endDate,
            acquiredCount = auction.auctionUserState.acquired_count,
            previewAttributes = previewAttrs,
            acquiredGiftsCount = auction.auctionUserState.acquired_count,
            isFinished = auction.isFinished
        )
    }

    fun mapAcquiredGift(giftId: Long, acquiredGift: TL_stars.TL_StarGiftAuctionAcquiredGift?): GiftAuctionAcquiredGiftModel? {
        if (acquiredGift == null) return null

        val peerId = acquiredGift.peer?.let { DialogObject.getPeerDialogId(it) } ?: 0L
        val text = acquiredGift.message?.text

        return GiftAuctionAcquiredGiftModel(
            giftId = giftId,
            num = acquiredGift.gift_num,
            date = acquiredGift.date.toLong(),
            peerId = peerId,
            bidAmount = acquiredGift.bid_amount,
            round = acquiredGift.round,
            pos = acquiredGift.pos,
            messageText = text
        )
    }

    fun toLegacyBidParams(domainParams: GiftAuctionBidParamsModel?): AuctionBidSheet.Params? {
        if (domainParams == null) return null

        val textWithEntities = domainParams.message?.let { msg ->
            TLRPC.TL_textWithEntities().apply {
                text = msg
            }
        }

        return AuctionBidSheet.Params(
            domainParams.dialogId,
            domainParams.hideName,
            textWithEntities
        )
    }
}
