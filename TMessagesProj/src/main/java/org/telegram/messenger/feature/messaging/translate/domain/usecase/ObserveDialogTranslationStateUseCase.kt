package org.telegram.messenger.feature.messaging.translate.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository

class ObserveDialogTranslationStateUseCase(
    private val repository: TranslationRepository
) {
    operator fun invoke(dialogId: Long): Flow<DialogTranslationStateModel> {
        return repository.observeDialogTranslationState(dialogId)
    }
}
