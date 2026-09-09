package org.telegram.messenger.feature.giftauctions.domain.usecase

import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class GetActiveAuctionsUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(): List<GiftAuctionModel> {
        return repository.getActiveAuctions()
    }
}
