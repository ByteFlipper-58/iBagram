package org.telegram.messenger.feature.businessbots.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository

class ObserveConnectedBotsUseCase(
    private val repository: BusinessBotsRepository
) {
    operator fun invoke(): Flow<List<ConnectedBotModel>> {
        return repository.observeConnectedBots()
    }
}
