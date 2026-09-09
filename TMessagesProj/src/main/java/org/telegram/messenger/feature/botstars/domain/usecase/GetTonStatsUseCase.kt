package org.telegram.messenger.feature.botstars.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository

class GetTonStatsUseCase(
    private val repository: BotStarsRepository
) {
    suspend operator fun invoke(dialogId: Long, force: Boolean = false): Result<BotStarsRevenueStatsModel?> {
        return repository.getTonStats(dialogId, force)
    }
}
