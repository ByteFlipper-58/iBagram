package org.telegram.messenger.feature.giftauctions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionAcquiredGiftModel
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class LoadAuctionAcquiredGiftsUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(giftId: Long): Result<List<GiftAuctionAcquiredGiftModel>> {
        return repository.loadAcquiredGifts(giftId)
    }
}
