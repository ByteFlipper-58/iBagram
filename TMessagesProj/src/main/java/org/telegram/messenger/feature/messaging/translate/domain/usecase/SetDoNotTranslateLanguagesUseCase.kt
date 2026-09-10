package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class SetDoNotTranslateLanguagesUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(languages: Set<String>): Result<Unit> {
        return repository.setDoNotTranslateLanguages(languages)
    }
}
