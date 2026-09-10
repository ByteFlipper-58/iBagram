package org.telegram.messenger.feature.botkeyboard.presentation

import org.telegram.messenger.feature.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.botkeyboard.domain.model.BotKeyboardLayout

sealed class BotKeyboardEvent {
    data class SelectMessage(val messageId: Long) : BotKeyboardEvent()
    data class PressButton(val messageId: Long, val button: BotButtonItem) : BotKeyboardEvent()
    data class SetLayout(val messageId: Long, val layout: BotKeyboardLayout) : BotKeyboardEvent()
    data class RemoveLayout(val messageId: Long) : BotKeyboardEvent()
    object ClearAll : BotKeyboardEvent()
    object DismissError : BotKeyboardEvent()
}
