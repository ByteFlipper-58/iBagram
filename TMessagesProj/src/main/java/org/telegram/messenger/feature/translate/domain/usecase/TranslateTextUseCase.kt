package org.telegram.messenger.feature.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.translate.domain.model.TranslationResultModel
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class TranslateTextUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(
        text: String,
        fromLanguage: String? = null,
        toLanguage: String
    ): Result<TranslationResultModel> {
        return repository.translateText(text, fromLanguage, toLanguage)
    }
}
