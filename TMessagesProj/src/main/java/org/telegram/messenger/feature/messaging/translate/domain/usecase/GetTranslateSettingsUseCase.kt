package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class GetTranslateSettingsUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(): Result<TranslateSettingsModel> {
        return repository.getTranslateSettings()
    }
}
