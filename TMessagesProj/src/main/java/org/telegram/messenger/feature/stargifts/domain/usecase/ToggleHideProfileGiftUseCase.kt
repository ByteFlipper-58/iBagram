package org.telegram.messenger.feature.stargifts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository

class ToggleHideProfileGiftUseCase(
    private val repository: StarGiftsRepository
) {
    suspend operator fun invoke(dialogId: Long, giftId: Long, hide: Boolean): Result<Boolean> {
        return repository.toggleHideGift(dialogId, giftId, hide)
    }
}
