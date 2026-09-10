package org.telegram.messenger.feature.business.giftauctions.domain.usecase

import org.telegram.messenger.feature.business.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository

class GetActiveAuctionsUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(): List<GiftAuctionModel> {
        return repository.getActiveAuctions()
    }
}
