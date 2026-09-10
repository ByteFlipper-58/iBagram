package org.telegram.messenger.feature.business.businessbots.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository

class DeleteConnectedBotUseCase(
    private val repository: BusinessBotsRepository
) {
    suspend operator fun invoke(botId: Long): Result<Unit> {
        return repository.deleteConnectedBot(botId)
    }
}
