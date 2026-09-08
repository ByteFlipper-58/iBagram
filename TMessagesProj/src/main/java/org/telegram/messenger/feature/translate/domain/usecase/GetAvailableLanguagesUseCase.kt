package org.telegram.messenger.feature.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.model.LanguageModel
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class GetAvailableLanguagesUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(): Result<List<LanguageModel>> {
        return repository.getAvailableLanguages()
    }
}
