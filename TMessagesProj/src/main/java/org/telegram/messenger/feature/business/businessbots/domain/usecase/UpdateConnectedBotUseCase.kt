package org.telegram.messenger.feature.business.businessbots.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository

class UpdateConnectedBotUseCase(
    private val repository: BusinessBotsRepository
) {
    suspend operator fun invoke(
        botId: Long,
        rights: BusinessBotRightsModel,
        recipients: BusinessBotRecipientsModel
    ): Result<ConnectedBotModel> {
        return repository.updateConnectedBot(botId, rights, recipients)
    }
}
