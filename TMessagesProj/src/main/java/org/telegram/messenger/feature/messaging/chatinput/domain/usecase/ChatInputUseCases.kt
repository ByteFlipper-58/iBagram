package org.telegram.messenger.feature.messaging.chatinput.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputRecordState
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputReplyQuote
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.messaging.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.messaging.chatinput.domain.model.TextFormatStyle
import org.telegram.messenger.feature.messaging.chatinput.domain.repository.ChatInputRepository

/**
 * Calculates whether the main action button is 'Send' or 'Record (Voice/Video)',
 * and whether the attachment button should be visible.
 */
class CalculateSendButtonStateUseCase {

    data class ButtonState(
        val isSendButton: Boolean,
        val isAttachButtonVisible: Boolean,
        val canSend: Boolean
    )

    operator fun invoke(
        text: String,
        replyQuote: ChatInputReplyQuote?,
        recordState: ChatInputRecordState
    ): ButtonState {
        val hasContent = text.isNotBlank() || (replyQuote?.isEdit == true)
        val isRecording = recordState.status != RecordStatus.IDLE

        val isSend = hasContent || isRecording
        val attachVisible = text.isEmpty() && !isRecording

        return ButtonState(
            isSendButton = isSend,
            isAttachButtonVisible = attachVisible,
            canSend = isSend
        )
    }
}

/**
 * Wraps or prepends markdown/entity tokens around the selected substring.
 */
class FormatTextSelectionUseCase {

    data class FormatResult(
        val newText: String,
        val newSelectionStart: Int,
        val newSelectionEnd: Int
    )

    operator fun invoke(
        originalText: String,
        selectionStart: Int,
        selectionEnd: Int,
        style: TextFormatStyle
    ): FormatResult {
        val start = selectionStart.coerceIn(0, originalText.length)
        val end = selectionEnd.coerceIn(0, originalText.length)
        val (selMin, selMax) = if (start <= end) start to end else end to start

        val prefix: String
        val suffix: String

        when (style) {
            TextFormatStyle.BOLD -> {
                prefix = "**"; suffix = "**"
            }
            TextFormatStyle.ITALIC -> {
                prefix = "__"; suffix = "__"
            }
            TextFormatStyle.MONO -> {
                prefix = "`"; suffix = "`"
            }
            TextFormatStyle.STRIKETHROUGH -> {
                prefix = "~~"; suffix = "~~"
            }
            TextFormatStyle.UNDERLINE -> {
                prefix = "--"; suffix = "--"
            }
            TextFormatStyle.SPOILER -> {
                prefix = "||"; suffix = "||"
            }
            TextFormatStyle.QUOTE -> {
                prefix = "> "; suffix = ""
            }
        }

        val selected = originalText.substring(selMin, selMax)
        val sb = StringBuilder()
        sb.append(originalText.substring(0, selMin))
        sb.append(prefix)
        sb.append(selected)
        sb.append(suffix)
        sb.append(originalText.substring(selMax))

        val newText = sb.toString()
        val newStart = selMin + prefix.length
        val newEnd = newStart + selected.length

        return FormatResult(
            newText = newText,
            newSelectionStart = newStart,
            newSelectionEnd = newEnd
        )
    }
}

/**
 * Validates whether starting or modifying a voice/round video recording is permitted.
 */
class ValidateVoiceRecordActionUseCase {

    sealed class RecordValidation {
        object Allowed : RecordValidation()
        data class Denied(val reason: String) : RecordValidation()
    }

    operator fun invoke(
        canRecordMedia: Boolean,
        currentStatus: RecordStatus,
        hasText: Boolean
    ): RecordValidation {
        if (!canRecordMedia) {
            return RecordValidation.Denied("Media recording restricted in this chat")
        }
        if (hasText && currentStatus == RecordStatus.IDLE) {
            return RecordValidation.Denied("Cannot record voice while draft text is present")
        }
        return RecordValidation.Allowed
    }
}

/**
 * Resolves panel toggle transitions (e.g. clicking emoji button when emoji panel is open returns KEYBOARD or NONE).
 */
class ResolvePanelVisibilityUseCase {

    operator fun invoke(
        currentMode: EnterViewPanelMode,
        requestedMode: EnterViewPanelMode
    ): EnterViewPanelMode {
        return if (currentMode == requestedMode) {
            EnterViewPanelMode.NONE
        } else {
            requestedMode
        }
    }
}

class ObserveChatInputStateUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke(): StateFlow<ChatInputState> = repository.observeState()
}

class GetChatInputStateUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke(): ChatInputState = repository.getState()
}

class SetChatInputTextUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke(text: String, selectionStart: Int = text.length, selectionEnd: Int = text.length) {
        repository.setText(text, selectionStart, selectionEnd)
    }
}

class SetChatInputPanelModeUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke(mode: EnterViewPanelMode) {
        repository.setPanelMode(mode)
    }
}

class SetChatInputReplyUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke(messageId: Long, quote: String? = null, quoteOffset: Int = 0) {
        repository.setReplyMessage(messageId, quote, quoteOffset)
    }
}

class ClearChatInputReplyUseCase(
    private val repository: ChatInputRepository
) {
    operator fun invoke() {
        repository.clearReplyOrEdit()
    }
}
