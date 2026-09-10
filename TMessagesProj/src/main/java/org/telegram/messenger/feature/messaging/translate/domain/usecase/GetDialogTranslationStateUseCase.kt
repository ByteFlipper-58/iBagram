package org.telegram.messenger.feature.messaging.translate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class GetDialogTranslationStateUseCase(
    private val repository: TranslationRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<DialogTranslationStateModel> {
        return repository.getDialogTranslationState(dialogId)
    }
}
