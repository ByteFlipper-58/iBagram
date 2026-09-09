package org.telegram.messenger.feature.botstars.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository

class ObserveConnectedStarBotsUseCase(
    private val repository: BotStarsRepository
) {
    operator fun invoke(dialogId: Long): Flow<List<ConnectedBotStarRefModel>> {
        return repository.observeConnectedBots(dialogId)
    }
}
