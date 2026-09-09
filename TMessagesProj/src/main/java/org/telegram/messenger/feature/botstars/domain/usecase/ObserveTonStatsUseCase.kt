package org.telegram.messenger.feature.botstars.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository

class ObserveTonStatsUseCase(
    private val repository: BotStarsRepository
) {
    operator fun invoke(dialogId: Long): Flow<BotStarsRevenueStatsModel?> {
        return repository.observeTonStats(dialogId)
    }
}
