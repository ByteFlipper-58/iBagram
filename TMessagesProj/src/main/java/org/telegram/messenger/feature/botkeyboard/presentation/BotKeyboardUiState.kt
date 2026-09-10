package org.telegram.messenger.feature.botkeyboard.presentation

import org.telegram.messenger.feature.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.botkeyboard.domain.model.BotKeyboardLayout

data class BotKeyboardUiState(
    val currentMessageId: Long = 0L,
    val currentLayout: BotKeyboardLayout? = null,
    val lastPressedButton: BotButtonItem? = null,
    val totalKeyboardsCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
