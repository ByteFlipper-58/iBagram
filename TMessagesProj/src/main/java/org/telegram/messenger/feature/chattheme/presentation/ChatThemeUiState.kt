package org.telegram.messenger.feature.chattheme.presentation

import org.telegram.messenger.feature.chattheme.domain.model.ChatThemeModel
import org.telegram.messenger.feature.chattheme.domain.model.DialogThemeStateModel

sealed interface ChatThemeUiState {
    data object Initial : ChatThemeUiState
    data object Loading : ChatThemeUiState
    data class Success(
        val dialogThemeState: DialogThemeStateModel,
        val availableThemes: List<ChatThemeModel> = emptyList(),
        val selectedTheme: ChatThemeModel? = null,
        val isSaving: Boolean = false,
        val error: String? = null
    ) : ChatThemeUiState
    data class Error(val message: String) : ChatThemeUiState
}
