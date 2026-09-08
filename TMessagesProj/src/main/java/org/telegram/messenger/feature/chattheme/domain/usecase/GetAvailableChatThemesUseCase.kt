package org.telegram.messenger.feature.chattheme.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository

class GetAvailableChatThemesUseCase(
    private val repository: ChatThemeRepository
) {
    suspend operator fun invoke(withDefault: Boolean = true): Result<List<ChatThemeModel>> {
        return repository.getAvailableThemes(withDefault)
    }
}
