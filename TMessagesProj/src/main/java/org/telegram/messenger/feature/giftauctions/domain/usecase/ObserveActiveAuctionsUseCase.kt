package org.telegram.messenger.feature.giftauctions.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class ObserveActiveAuctionsUseCase(
    private val repository: GiftAuctionsRepository
) {
    operator fun invoke(): Flow<List<GiftAuctionModel>> {
        return repository.observeActiveAuctions()
    }
}
