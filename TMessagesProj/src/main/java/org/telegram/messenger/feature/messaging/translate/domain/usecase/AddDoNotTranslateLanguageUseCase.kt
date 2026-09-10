package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class AddDoNotTranslateLanguageUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(languageCode: String): Result<Unit> {
        return repository.addDoNotTranslateLanguage(languageCode)
    }
}
