package org.telegram.messenger.feature.business.giftauctions.data.datasource

import org.telegram.messenger.GiftAuctionController
import org.telegram.ui.Gifts.AuctionBidSheet

/**
 * Local data source for caching and subscribing to Gift Auction controller state.
 */
open class GiftAuctionsLocalDataSource(
    private val currentAccount: Int
) {

    open fun getController(): GiftAuctionController? {
        return try {
            GiftAuctionController.getInstance(currentAccount)
        } catch (_: Throwable) {
            null
        }
    }

    open fun getActiveAuctions(): List<GiftAuctionController.Auction> {
        return getController()?.activeAuctions ?: emptyList()
    }

    open fun getAuction(giftId: Long): GiftAuctionController.Auction? {
        return getController()?.getAuction(giftId)
    }

    open fun sendBid(
        giftId: Long,
        params: AuctionBidSheet.Params?,
        amount: Long,
        callback: (Boolean, String?) -> Unit
    ) {
        val controller = getController()
        if (controller == null) {
            callback(false, "Controller unavailable")
            return
        }
        controller.sendBid(giftId, params, amount) { success, error ->
            callback(success, error)
        }
    }

    open fun subscribeToActiveAuctions(listener: GiftAuctionController.OnActiveAuctionsUpdateListeners) {
        getController()?.subscribeToActiveAuctionsUpdates(listener)
    }

    open fun unsubscribeFromActiveAuctions(listener: GiftAuctionController.OnActiveAuctionsUpdateListeners) {
        getController()?.unsubscribeFromActiveAuctionsUpdates(listener)
    }

    open fun subscribeToAuction(
        giftId: Long,
        listener: GiftAuctionController.OnAuctionUpdateListener
    ): GiftAuctionController.Auction? {
        return getController()?.subscribeToGiftAuction(giftId, listener)
    }

    open fun unsubscribeFromAuction(
        giftId: Long,
        listener: GiftAuctionController.OnAuctionUpdateListener
    ) {
        getController()?.unsubscribeFromGiftAuction(giftId, listener)
    }
}
