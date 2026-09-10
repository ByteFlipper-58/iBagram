package org.telegram.messenger.feature.messaging.chattheme.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chattheme.domain.repository.ChatThemeRepository

class SetDialogThemeUseCase(
    private val repository: ChatThemeRepository
) {
    suspend operator fun invoke(
        dialogId: Long,
        emoticon: String?,
        giftSlug: String? = null
    ): Result<Unit> {
        return repository.setDialogTheme(dialogId, emoticon, giftSlug)
    }
}
