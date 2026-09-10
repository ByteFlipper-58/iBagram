package org.telegram.messenger.feature.chatinput.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.chatinput.domain.model.RecordType

/**
 * Domain repository contract managing the state and operations of the ChatActivityEnterView.
 */
interface ChatInputRepository {

    fun getState(): ChatInputState

    fun observeState(): StateFlow<ChatInputState>

    fun setText(text: String, selectionStart: Int = text.length, selectionEnd: Int = text.length)

    fun setPanelMode(mode: EnterViewPanelMode)

    fun setReplyMessage(messageId: Long, quote: String? = null, quoteOffset: Int = 0)

    fun setEditMessage(messageId: Long, text: String)

    fun clearReplyOrEdit()

    fun updateSendOptions(options: ChatInputSendOptions)

    fun startRecording(type: RecordType)

    fun lockRecording()

    fun pauseRecording()

    fun resumeRecording()

    fun cancelRecording()

    fun stopRecording()

    fun updateRecordProgress(durationMs: Long, amplitude: Float)

    fun toggleVoiceOnce()

    fun reset()
}
