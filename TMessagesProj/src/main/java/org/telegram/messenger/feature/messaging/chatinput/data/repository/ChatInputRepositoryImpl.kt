package org.telegram.messenger.feature.messaging.chatinput.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.chatinput.data.datasource.ChatInputLocalDataSource
import org.telegram.messenger.feature.messaging.chatinput.data.datasource.ChatInputRemoteDataSource
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputSendOptions
import org.telegram.messenger.feature.messaging.chatinput.domain.model.ChatInputState
import org.telegram.messenger.feature.messaging.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.messaging.chatinput.domain.repository.ChatInputRepository

class ChatInputRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: ChatInputLocalDataSource,
    private val remoteDataSource: ChatInputRemoteDataSource
) : ChatInputRepository {

    override fun getState(): ChatInputState = localDataSource.getState()

    override fun observeState(): StateFlow<ChatInputState> = localDataSource.state

    override fun setText(text: String, selectionStart: Int, selectionEnd: Int) {
        localDataSource.setText(text, selectionStart, selectionEnd)
    }

    override fun setPanelMode(mode: EnterViewPanelMode) {
        localDataSource.setPanelMode(mode)
    }

    override fun setReplyMessage(messageId: Long, quote: String?, quoteOffset: Int) {
        localDataSource.setReplyMessage(messageId, quote, quoteOffset)
    }

    override fun setEditMessage(messageId: Long, text: String) {
        localDataSource.setEditMessage(messageId, text)
    }

    override fun clearReplyOrEdit() {
        localDataSource.clearReplyOrEdit()
    }

    override fun updateSendOptions(options: ChatInputSendOptions) {
        localDataSource.updateSendOptions(options)
    }

    override fun startRecording(type: RecordType) {
        localDataSource.startRecording(type)
    }

    override fun lockRecording() {
        localDataSource.lockRecording()
    }

    override fun pauseRecording() {
        localDataSource.pauseRecording()
    }

    override fun resumeRecording() {
        localDataSource.resumeRecording()
    }

    override fun cancelRecording() {
        localDataSource.cancelRecording()
    }

    override fun stopRecording() {
        localDataSource.stopRecording()
    }

    override fun updateRecordProgress(durationMs: Long, amplitude: Float) {
        localDataSource.updateRecordProgress(durationMs, amplitude)
    }

    override fun toggleVoiceOnce() {
        localDataSource.toggleVoiceOnce()
    }

    override fun reset() {
        localDataSource.reset()
    }
}
