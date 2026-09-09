package org.telegram.messenger.feature.giftauctions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository

class RefreshActiveAuctionsUseCase(
    private val repository: GiftAuctionsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.refreshActiveAuctions()
    }
}
