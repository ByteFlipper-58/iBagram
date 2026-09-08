package org.telegram.messenger.feature.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class GetTranslateSettingsUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(): Result<TranslateSettingsModel> {
        return repository.getTranslateSettings()
    }
}
