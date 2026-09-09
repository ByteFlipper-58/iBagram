package org.telegram.messenger.feature.botstars.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository

class LoadConnectedStarBotsUseCase(
    private val repository: BotStarsRepository
) {
    suspend operator fun invoke(dialogId: Long, reload: Boolean = false): Result<List<ConnectedBotStarRefModel>> {
        return repository.loadConnectedBots(dialogId, reload)
    }
}
