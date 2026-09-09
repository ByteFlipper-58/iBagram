package org.telegram.messenger.feature.stargifts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository

class TogglePinProfileGiftUseCase(
    private val repository: StarGiftsRepository
) {
    suspend operator fun invoke(dialogId: Long, giftId: Long, pin: Boolean): Result<Boolean> {
        return repository.togglePinGift(dialogId, giftId, pin)
    }
}
