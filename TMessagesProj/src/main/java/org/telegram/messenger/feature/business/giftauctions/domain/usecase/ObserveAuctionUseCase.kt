package org.telegram.messenger.feature.business.giftauctions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository

class ObserveAuctionUseCase(
    private val repository: GiftAuctionsRepository
) {
    operator fun invoke(giftId: Long): Flow<GiftAuctionModel?> {
        return repository.observeAuction(giftId)
    }
}
