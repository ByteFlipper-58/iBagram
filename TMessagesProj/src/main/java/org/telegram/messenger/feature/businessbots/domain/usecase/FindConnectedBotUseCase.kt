package org.telegram.messenger.feature.businessbots.domain.usecase

import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository

class FindConnectedBotUseCase(
    private val repository: BusinessBotsRepository
) {
    operator fun invoke(botId: Long): ConnectedBotModel? {
        return repository.findConnectedBot(botId)
    }
}
