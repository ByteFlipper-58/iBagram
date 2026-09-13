package org.telegram.messenger.feature.messaging.botkeyboard.data.datasource

import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_keyboard
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe local data source for in-memory and cached bot keyboards.
 */
class BotKeyboardLocalDataSource(
    private val account: Int
) {
    private val inMemoryKeyboards = ConcurrentHashMap<Long, BotKeyboardLayout>()

    fun getKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        return inMemoryKeyboards[messageId]
    }

    fun setKeyboardForMessage(messageId: Long, layout: BotKeyboardLayout) {
        inMemoryKeyboards[messageId] = layout
    }

    fun removeKeyboardForMessage(messageId: Long): BotKeyboardLayout? {
        return inMemoryKeyboards.remove(messageId)
    }

    fun clearAllKeyboards() {
        inMemoryKeyboards.clear()
    }

    fun isForceReply(replyMarkup: Any?): Boolean {
        if (replyMarkup is TLRPC.ReplyMarkup) {
            return TLKeyboardHelper.isForceReply(replyMarkup)
        }
        return false
    }

    fun isButtonWebView(button: Any?): Boolean {
        if (button is TL_keyboard.KeyboardButtonProto) {
            return TLKeyboardHelper.isButtonWebView(button)
        }
        return false
    }

    fun getAllKeyboards(): Map<Long, BotKeyboardLayout> {
        return HashMap(inMemoryKeyboards)
    }
}
