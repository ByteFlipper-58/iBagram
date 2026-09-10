package org.telegram.messenger.feature.business.giftauctions.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel

interface GiftAuctionsRepository {
    fun observeActiveAuctions(): Flow<List<GiftAuctionModel>>
    fun observeAuction(giftId: Long): Flow<GiftAuctionModel?>
    suspend fun getActiveAuctions(): List<GiftAuctionModel>
    suspend fun getAuctionById(giftId: Long): Result<GiftAuctionModel>
    suspend fun getAuctionBySlug(slug: String): Result<GiftAuctionModel>
    suspend fun sendBid(giftId: Long, amount: Long, params: GiftAuctionBidParamsModel? = null): Result<Unit>
    suspend fun loadAcquiredGifts(giftId: Long): Result<List<GiftAuctionAcquiredGiftModel>>
    suspend fun refreshActiveAuctions(): Result<Unit>
}
