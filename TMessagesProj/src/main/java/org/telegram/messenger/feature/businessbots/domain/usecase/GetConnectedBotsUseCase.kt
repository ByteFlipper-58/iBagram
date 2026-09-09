package org.telegram.messenger.feature.businessbots.domain.usecase

import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository

class GetConnectedBotsUseCase(
    private val repository: BusinessBotsRepository
) {
    suspend operator fun invoke(): List<ConnectedBotModel> {
        return repository.getConnectedBots()
    }
}
