package org.telegram.messenger.feature.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class RemoveDoNotTranslateLanguageUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(languageCode: String): Result<Unit> {
        return repository.removeDoNotTranslateLanguage(languageCode)
    }
}
