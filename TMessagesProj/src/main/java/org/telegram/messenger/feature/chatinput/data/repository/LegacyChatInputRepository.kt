package org.telegram.messenger.feature.chatinput.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputRecordState
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputReplyQuote
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.chatinput.domain.repository.ChatInputRepository
import org.telegram.messenger.feature.chatinput.domain.usecase.CalculateSendButtonStateUseCase

/**
 * Thread-safe StateFlow implementation of ChatInputRepository adapting ChatActivityEnterView state.
 */
class LegacyChatInputRepository(
    private val sendButtonStateUseCase: CalculateSendButtonStateUseCase = CalculateSendButtonStateUseCase()
) : ChatInputRepository {

    private val _state = MutableStateFlow(ChatInputState())
    private val lock = Any()

    override fun getState(): ChatInputState {
        synchronized(lock) {
            return _state.value
        }
    }

    override fun observeState(): StateFlow<ChatInputState> {
        return _state.asStateFlow()
    }

    override fun setText(text: String, selectionStart: Int, selectionEnd: Int) {
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

    override fun setPanelMode(mode: EnterViewPanelMode) {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(panelMode = mode)
        }
    }

    override fun setReplyMessage(messageId: Long, quote: String?, quoteOffset: Int) {
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

    override fun setEditMessage(messageId: Long, text: String) {
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

    override fun clearReplyOrEdit() {
        synchronized(lock) {
            val current = _state.value
            val buttonState = sendButtonStateUseCase(current.text, null, current.recordState)
            _state.value = current.copy(
                replyQuote = null,
                canSend = buttonState.canSend
            )
        }
    }

    override fun updateSendOptions(options: ChatInputSendOptions) {
        synchronized(lock) {
            val current = _state.value
            _state.value = current.copy(sendOptions = options)
        }
    }

    override fun startRecording(type: RecordType) {
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

    override fun lockRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.RECORDING) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.LOCKED)
                )
            }
        }
    }

    override fun pauseRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.RECORDING || current.recordState.status == RecordStatus.LOCKED) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.PAUSED)
                )
            }
        }
    }

    override fun resumeRecording() {
        synchronized(lock) {
            val current = _state.value
            if (current.recordState.status == RecordStatus.PAUSED) {
                _state.value = current.copy(
                    recordState = current.recordState.copy(status = RecordStatus.LOCKED)
                )
            }
        }
    }

    override fun cancelRecording() {
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

    override fun stopRecording() {
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

    override fun updateRecordProgress(durationMs: Long, amplitude: Float) {
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

    override fun toggleVoiceOnce() {
        synchronized(lock) {
            val current = _state.value
            val newOnce = !current.recordState.isVoiceOnce
            _state.value = current.copy(
                recordState = current.recordState.copy(isVoiceOnce = newOnce),
                sendOptions = current.sendOptions.copy(ttlSeconds = if (newOnce) 1 else 0)
            )
        }
    }

    override fun reset() {
        synchronized(lock) {
            _state.value = ChatInputState()
        }
    }
}
