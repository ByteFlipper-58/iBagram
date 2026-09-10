package org.telegram.messenger.feature.botkeyboard.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.botkeyboard.domain.model.BotKeyboardState
import org.telegram.messenger.feature.botkeyboard.domain.repository.BotKeyboardRepository
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_keyboard
import java.util.concurrent.ConcurrentHashMap

class LegacyBotKeyboardRepository(
    private val currentAccount: Int
) : BotKeyboardRepository {

    private val inMemoryKeyboards = ConcurrentHashMap<Long, BotKeyboardLayout>()
    private val _state = MutableStateFlow(BotKeyboardState())

    override fun getKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        return inMemoryKeyboards[messageId]
    }

    override fun setKeyboardForMessage(messageId: Long, layout: BotKeyboardLayout) {
        inMemoryKeyboards[messageId] = layout
        syncState()
    }

    override fun removeKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        val removed = inMemoryKeyboards.remove(messageId)
        syncState()
        return removed
    }

    override fun clearAllKeyboards() {
        inMemoryKeyboards.clear()
        syncState()
    }

    override fun isForceReply(replyMarkup: Any?): Boolean {
        if (replyMarkup is TLRPC.ReplyMarkup) {
            return TLKeyboardHelper.isForceReply(replyMarkup)
        }
        return false
    }

    override fun isButtonWebView(button: Any?): Boolean {
        if (button is TL_keyboard.KeyboardButtonProto) {
            return TLKeyboardHelper.isButtonWebView(button)
        }
        return false
    }

    override fun recordButtonPressed(button: BotButtonItem) {
        _state.update { current ->
            current.copy(lastPressedButton = button)
        }
    }

    override fun observeState(): StateFlow<BotKeyboardState> {
        return _state.asStateFlow()
    }

    override fun getCurrentState(): BotKeyboardState {
        return _state.value
    }

    private fun syncState() {
        _state.update { current ->
            current.copy(activeKeyboards = HashMap(inMemoryKeyboards))
        }
    }
}
