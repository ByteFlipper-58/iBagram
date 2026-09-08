package org.telegram.messenger.feature.chattheme.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository

class GetDialogThemeStateUseCase(
    private val repository: ChatThemeRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<DialogThemeStateModel> {
        return repository.getDialogThemeState(dialogId)
    }
}
