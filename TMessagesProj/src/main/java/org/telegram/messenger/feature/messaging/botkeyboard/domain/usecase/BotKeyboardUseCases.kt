package org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotCustomButtonType
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardRow
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardState
import org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository

class BuildBotKeyboardLayoutUseCase {
    operator fun invoke(rows: List<List<BotButtonItem>>, separators: Int = 0): BotKeyboardLayout {
        val keyboardRows = rows.mapIndexed { index, buttons ->
            val hasSeparator = (separators and (1 shl index)) != 0
            BotKeyboardRow(buttons = buttons, hasSeparator = hasSeparator)
        }
        return BotKeyboardLayout(rows = keyboardRows)
    }
}

class CheckIsForceReplyUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(replyMarkup: Any?): Boolean {
        return repository.isForceReply(replyMarkup)
    }
}

class CheckIsButtonWebViewUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(button: Any?): Boolean {
        return repository.isButtonWebView(button)
    }
}

class ResolveCustomButtonTypeUseCase {
    operator fun invoke(id: Int): BotCustomButtonType? {
        return BotCustomButtonType.fromId(id)
    }
}

class GetKeyboardForMessageUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(messageId: Long): BotKeyboardLayout? {
        return repository.getKeyboardForMessage(messageId)
    }
}

class SetKeyboardForMessageUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(messageId: Long, layout: BotKeyboardLayout) {
        repository.setKeyboardForMessage(messageId, layout)
    }
}

class RemoveKeyboardForMessageUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(messageId: Long): BotKeyboardLayout? {
        return repository.removeKeyboardForMessage(messageId)
    }
}

class ClearAllKeyboardsUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke() {
        repository.clearAllKeyboards()
    }
}

class RecordButtonPressedUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(button: BotButtonItem) {
        repository.recordButtonPressed(button)
    }
}

class ObserveBotKeyboardStateUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(): StateFlow<BotKeyboardState> {
        return repository.observeState()
    }
}

class GetBotKeyboardStateUseCase(
    private val repository: BotKeyboardRepository
) {
    operator fun invoke(): BotKeyboardState {
        return repository.getCurrentState()
    }
}
