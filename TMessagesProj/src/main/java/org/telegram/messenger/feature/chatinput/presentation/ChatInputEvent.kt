package org.telegram.messenger.feature.chatinput.presentation

import org.telegram.messenger.feature.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.chatinput.domain.model.TextFormatStyle

/**
 * UI Events for ChatActivityEnterView interactions.
 */
sealed class ChatInputEvent {

    data class TextChanged(
        val text: String,
        val selectionStart: Int = text.length,
        val selectionEnd: Int = text.length
    ) : ChatInputEvent()

    data class ApplyTextFormat(val style: TextFormatStyle) : ChatInputEvent()

    data class TogglePanel(val mode: EnterViewPanelMode) : ChatInputEvent()

    data class SetReply(
        val messageId: Long,
        val quote: String? = null,
        val quoteOffset: Int = 0
    ) : ChatInputEvent()

    data class SetEdit(val messageId: Long, val text: String) : ChatInputEvent()

    object ClearReplyOrEdit : ChatInputEvent()

    data class UpdateSendOptions(val options: ChatInputSendOptions) : ChatInputEvent()

    data class StartRecording(val type: RecordType) : ChatInputEvent()

    object LockRecording : ChatInputEvent()

    object PauseRecording : ChatInputEvent()

    object ResumeRecording : ChatInputEvent()

    object CancelRecording : ChatInputEvent()

    object StopRecording : ChatInputEvent()

    data class RecordProgressUpdated(val durationMs: Long, val amplitude: Float) : ChatInputEvent()

    object ToggleVoiceOnce : ChatInputEvent()

    object Reset : ChatInputEvent()
}
