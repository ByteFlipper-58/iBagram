package org.telegram.messenger.feature.business.giftauctions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository

class GetAuctionByIdUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(giftId: Long): Result<GiftAuctionModel> {
        return repository.getAuctionById(giftId)
    }
}
