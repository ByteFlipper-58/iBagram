package org.telegram.messenger.feature.business.botstars.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository

class GetAdminedBotsAndChannelsUseCase(
    private val repository: BotStarsRepository
) {
    suspend fun getAdminedBots(): Result<List<Long>> {
        return repository.loadAdminedBots()
    }

    suspend fun getAdminedChannels(): Result<List<Long>> {
        return repository.loadAdminedChannels()
    }
}
