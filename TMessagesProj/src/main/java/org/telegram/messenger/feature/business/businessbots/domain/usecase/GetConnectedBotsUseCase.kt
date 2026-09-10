package org.telegram.messenger.feature.business.businessbots.domain.usecase

import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository

class GetConnectedBotsUseCase(
    private val repository: BusinessBotsRepository
) {
    suspend operator fun invoke(): List<ConnectedBotModel> {
        return repository.getConnectedBots()
    }
}
