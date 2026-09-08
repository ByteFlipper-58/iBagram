package org.telegram.messenger.feature.chattheme.presentation

import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel

sealed interface ChatThemeEvent {
    data class LoadThemes(val dialogId: Long) : ChatThemeEvent
    data class SelectTheme(val theme: ChatThemeModel) : ChatThemeEvent
    data class ApplyTheme(val dialogId: Long, val theme: ChatThemeModel) : ChatThemeEvent
    data class ResetTheme(val dialogId: Long) : ChatThemeEvent
}
