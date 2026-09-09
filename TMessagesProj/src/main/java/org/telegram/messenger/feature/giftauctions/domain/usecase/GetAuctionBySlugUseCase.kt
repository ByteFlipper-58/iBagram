package org.telegram.messenger.feature.giftauctions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.giftauctions.domain.model.GiftAuctionModel
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class GetAuctionBySlugUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(slug: String): Result<GiftAuctionModel> {
        return repository.getAuctionBySlug(slug)
    }
}
