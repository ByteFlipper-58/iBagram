package org.telegram.messenger.feature.businessbots.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository

class LoadConnectedBotsUseCase(
    private val repository: BusinessBotsRepository
) {
    suspend operator fun invoke(forceReload: Boolean = false): Result<List<ConnectedBotModel>> {
        return repository.loadConnectedBots(forceReload)
    }
}
