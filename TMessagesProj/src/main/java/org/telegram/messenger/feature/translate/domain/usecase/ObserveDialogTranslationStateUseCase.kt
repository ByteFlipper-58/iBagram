package org.telegram.messenger.feature.translate.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.translate.domain.model.DialogTranslationStateModel
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository

class ObserveDialogTranslationStateUseCase(
    private val repository: TranslationRepository
) {
    operator fun invoke(dialogId: Long): Flow<DialogTranslationStateModel> {
        return repository.observeDialogTranslationState(dialogId)
    }
}
