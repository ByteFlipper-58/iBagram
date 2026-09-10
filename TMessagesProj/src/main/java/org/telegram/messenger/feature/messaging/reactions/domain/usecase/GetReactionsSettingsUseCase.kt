package org.telegram.messenger.feature.messaging.reactions.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.reactions.domain.model.ReactionsSettingsModel
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository

class GetReactionsSettingsUseCase(
    private val repository: ReactionsRepository
) {
    suspend operator fun invoke(): Result<ReactionsSettingsModel> {
        return repository.getReactionsSettings()
    }
}
