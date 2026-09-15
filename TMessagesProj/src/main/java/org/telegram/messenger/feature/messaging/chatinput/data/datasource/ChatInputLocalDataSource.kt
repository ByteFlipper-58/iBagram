package org.telegram.messenger.feature.messaging.chatinput.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputRecordState
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputReplyQuote
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.messaging.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.CalculateSendButtonStateUseCase

class ChatInputLocalDataSource(
    private val currentAccount: Int = 0,
    private val sendButtonStateUseCase: CalculateSendButtonStateUseCase = CalculateSendButtonStateUseCase()
) {
    private val _state = MutableStateFlow(ChatInputState())
    val state: StateFlow<ChatInputState> = _state.asStateFlow()
    private val lock = Any()

    fun getState(): ChatInputState {
        synchronized(lock) {
            return _state.value
        }
    }

    fun setText(text: String, selectionStart: Int = text.length, selectionEnd: Int = text.length) {
        synchronized(lock) {
            val current = _state.value
            val buttonState = sendButtonStateUseCase(text, current.replyQuote, current.recordState)
            _state.value = current.copy(
                text = text,
                selectionStart = selectionStart.coerceIn(0, text.length),
                selectionEnd = selectionEnd.coerceIn(0, text.length),
                isAttachButtonVisible = buttonState.isAttachButtonVisible,
                canSend = buttonState.canSend
            )
        }
    }

    fun setPanelMode(mode: EnterViewPanelMode) {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(panelMode = mode)
        }
    }

    fun setReplyMessage(messageId: Long, quote: String? = null, quoteOffset: Int = 0) {
        synchronized(lock) {
            val current = _state.value
            val replyQuote = ChatInputReplyQuote(
                messageId = messageId,
                isEdit = false,
                text = null,
                quote = quote,
                quoteOffset = quoteOffset
            )
            val buttonState = sendButtonStateUseCase(current.text, replyQuote, current.recordState)
            _state.value = current.copy(
                replyQuote = replyQuote,
                canSend = buttonState.canSend
            )
        }
    }

    fun setEditMessage(messageId: Long, text: String) {
        synchronized(lock) {
            val current = _state.value
            val replyQuote = ChatInputReplyQuote(
                messageId = messageId,
                isEdit = true,
                text = text,
                quote = null,
                quoteOffset = 0
            )
            val buttonState = sendButtonStateUseCase(current.text, replyQuote, current.recordState)
            _state.value = current.copy(
                text = text,
                selectionStart = text.length,
                selectionEnd = text.length,
                replyQuote = replyQuote,
                canSend = buttonState.canSend
            )
        }
    }

    fun clearReplyOrEdit() {
        synchronized(lock) {
            val current = _state.value
            val buttonState = sendButtonStateUseCase(current.text, null, current.recordState)
            _state.value = current.copy(
                replyQuote = null,
                canSend = buttonState.canSend
            )
        }
    }

    fun updateSendOptions(options: ChatInputSendOptions) {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(sendOptions = options)
        }
    }

    fun startRecording(type: RecordType) {
        synchronized(lock) {
            val current = _state.value
            val record = ChatInputRecordState(
                type = type,
                status = RecordStatus.RECORDING,
                durationMs = 0L,
                amplitude = 0.0f,
                isVoiceOnce = false
            )
            val buttonState = sendButtonStateUseCase(current.text, current.replyQuote, record)
            _state.value = current.copy(
                recordState = record,
                isAttachButtonVisible = buttonState.isAttachButtonVisible,
                canSend = buttonState.canSend
            )
        }
    }

    fun lockRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.RECORDING) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.LOCKED)
                )
            }
        }
    }

    fun pauseRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.RECORDING || current.recordState.status == RecordStatus.LOCKED) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.PAUSED)
                )
            }
        }
    }

    fun resumeRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.PAUSED) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.LOCKED)
                )
            }
        }
    }

    fun cancelRecording() {
        synchronized(lock) {
            val current = _state.value
            val idleRecord = ChatInputRecordState(status = RecordStatus.IDLE)
            val buttonState = sendButtonStateUseCase(current.text, current.replyQuote, idleRecord)
            _state.value = current.copy(
                recordState = idleRecord,
                isAttachButtonVisible = buttonState.isAttachButtonVisible,
                canSend = buttonState.canSend
            )
        }
    }

    fun stopRecording() {
        synchronized(lock) {
            val current = _state.value
            val previewRecord = current.recordState.copy(status = RecordStatus.PREVIEW)
            val buttonState = sendButtonStateUseCase(current.text, current.replyQuote, previewRecord)
            _state.value = current.copy(
                recordState = previewRecord,
                canSend = buttonState.canSend
            )
        }
    }

    fun updateRecordProgress(durationMs: Long, amplitude: Float) {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status != RecordStatus.IDLE) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(
                        durationMs = durationMs,
                        amplitude = amplitude
                    )
                )
            }
        }
    }

    fun toggleVoiceOnce() {
        synchronized(lock) {
            val current = _state.value
            val newOnce = !current.recordState.isVoiceOnce
            _state.value = current.copy(
                recordState = current.recordState.copy(isVoiceOnce = newOnce),
                sendOptions = current.sendOptions.copy(ttlSeconds = if (newOnce) 1 else 0)
            )
        }
    }

    fun reset() {
        synchronized(lock) {
            _state.value = ChatInputState()
        }
    }
}
