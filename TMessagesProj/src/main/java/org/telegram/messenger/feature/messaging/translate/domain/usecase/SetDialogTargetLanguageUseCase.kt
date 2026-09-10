package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class SetDialogTargetLanguageUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(dialogId: Long, languageCode: String): Result<Unit> {
        return repository.setDialogTranslateTargetLanguage(dialogId, languageCode)
    }
}
