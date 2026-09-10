package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class ToggleDialogTranslatingUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(dialogId: Long, enabled: Boolean): Result<Unit> {
        return repository.toggleDialogTranslating(dialogId, enabled)
    }
}
