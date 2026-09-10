package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class GetAvailableLanguagesUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(): Result<List<LanguageModel>> {
        return repository.getAvailableLanguages()
    }
}
