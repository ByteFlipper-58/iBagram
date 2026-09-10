package org.telegram.messenger.feature.messaging.translate.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.translate.domain.model.TranslateSettingsModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class ObserveTranslateSettingsUseCase(
    private val repository: TranslationRepository
) {
    operator fun invoke(): Flow<TranslateSettingsModel> {
        return repository.observeTranslateSettings()
    }
}
