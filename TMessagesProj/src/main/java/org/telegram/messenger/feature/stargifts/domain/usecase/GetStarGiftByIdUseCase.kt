package org.telegram.messenger.feature.stargifts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.domain.model.StarGiftModel
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository

class GetStarGiftByIdUseCase(
    private val repository: StarGiftsRepository
) {
    suspend operator fun invoke(giftId: Long): Result<StarGiftModel?> {
        return repository.getGift(giftId)
    }
}
