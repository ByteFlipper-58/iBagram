package org.telegram.messenger.feature.chattheme.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository

class ObserveDialogThemeUseCase(
    private val repository: ChatThemeRepository
) {
    operator fun invoke(dialogId: Long): Flow<DialogThemeStateModel> {
        return repository.observeDialogTheme(dialogId)
    }
}
