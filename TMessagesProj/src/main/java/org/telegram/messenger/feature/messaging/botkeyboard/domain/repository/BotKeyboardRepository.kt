package org.telegram.messenger.feature.messaging.botkeyboard.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardState

interface BotKeyboardRepository {
    fun getKeyboardForMessage(messageId: Long): BotKeyboardLayout?
    fun setKeyboardForMessage(messageId: Long, layout: BotKeyboardLayout)
    fun removeKeyboardForMessage(messageId: Long): BotKeyboardLayout?
    fun clearAllKeyboards()

    fun isForceReply(replyMarkup: Any?): Boolean
    fun isButtonWebView(button: Any?): Boolean

    fun observeState(): StateFlow<BotKeyboardState>
    fun getCurrentState(): BotKeyboardState
    fun recordButtonPressed(button: BotButtonItem)
}
