package org.telegram.messenger.feature.giftauctions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionBidParamsModel
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class SendAuctionBidUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(
        giftId: Long,
        amount: Long,
        params: GiftAuctionBidParamsModel? = null
    ): Result<Unit> {
        return repository.sendBid(giftId, amount, params)
    }
}
