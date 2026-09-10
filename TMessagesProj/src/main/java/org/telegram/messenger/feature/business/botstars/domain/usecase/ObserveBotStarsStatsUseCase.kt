package org.telegram.messenger.feature.business.botstars.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository

class ObserveBotStarsStatsUseCase(
    private val repository: BotStarsRepository
) {
    operator fun invoke(dialogId: Long): Flow<BotStarsRevenueStatsModel?> {
        return repository.observeBotStarsStats(dialogId)
    }
}
