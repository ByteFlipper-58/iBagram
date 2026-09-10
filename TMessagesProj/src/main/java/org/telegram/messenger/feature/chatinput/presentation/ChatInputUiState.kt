package org.telegram.messenger.feature.chatinput.presentation

import org.telegram.messenger.feature.chatinput.data.mapper.ChatInputMapper
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputRecordState
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputReplyQuote
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.chatinput.domain.model.RecordStatus

/**
 * UI State for ChatActivityEnterView presentation layer.
 */
data class ChatInputUiState(
    val text: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val panelMode: EnterViewPanelMode = EnterViewPanelMode.NONE,
    val replyQuote: ChatInputReplyQuote? = null,
    val sendOptions: ChatInputSendOptions = ChatInputSendOptions(),
    val recordState: ChatInputRecordState = ChatInputRecordState(),
    val isAttachButtonVisible: Boolean = true,
    val canSend: Boolean = false
) {
    val isRecording: Boolean
        get() = recordState.status != RecordStatus.IDLE

    val formattedRecordDuration: String
        get() = ChatInputMapper.formatRecordDuration(recordState.durationMs)

    val panelName: String
        get() = ChatInputMapper.getPanelName(panelMode)

    val recordStatusName: String
        get() = ChatInputMapper.getRecordStatusName(recordState.status)

    companion object {
        fun fromDomain(domainState: ChatInputState): ChatInputUiState {
            return ChatInputUiState(
                text = domainState.text,
                selectionStart = domainState.selectionStart,
                selectionEnd = domainState.selectionEnd,
                panelMode = domainState.panelMode,
                replyQuote = domainState.replyQuote,
                sendOptions = domainState.sendOptions,
                recordState = domainState.recordState,
                isAttachButtonVisible = domainState.isAttachButtonVisible,
                canSend = domainState.canSend
            )
        }
    }
}
