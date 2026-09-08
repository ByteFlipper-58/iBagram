package org.telegram.messenger.feature.chattheme.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository

class ResetDialogThemeUseCase(
    private val repository: ChatThemeRepository
) {
    suspend operator fun invoke(dialogId: Long): Result<Unit> {
        return repository.resetDialogTheme(dialogId)
    }
}
