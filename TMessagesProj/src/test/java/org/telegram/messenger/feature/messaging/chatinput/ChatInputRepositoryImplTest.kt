package org.telegram.messenger.feature.messaging.chatinput

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.chatinput.data.datasource.ChatInputLocalDataSource
import org.telegram.messenger.feature.messaging.chatinput.data.datasource.ChatInputRemoteDataSource
import org.telegram.messenger.feature.messaging.chatinput.data.repository.ChatInputRepositoryImpl
import org.telegram.messenger.feature.messaging.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.messaging.chatinput.domain.model.RecordType

class ChatInputRepositoryImplTest {

    private lateinit var localDataSource: ChatInputLocalDataSource
    private lateinit var remoteDataSource: ChatInputRemoteDataSource
    private lateinit var repository: ChatInputRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ChatInputLocalDataSource(0)
        remoteDataSource = ChatInputRemoteDataSource(0)
        repository = ChatInputRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_hasDefaultValues() {
        val state = repository.getState()
        assertEquals("", state.text)
        assertFalse(state.canSend)
        assertTrue(state.isAttachButtonVisible)
        assertEquals(EnterViewPanelMode.NONE, state.panelMode)
        assertEquals(RecordStatus.IDLE, state.recordState.status)
        assertNull(state.replyQuote)
    }

    @Test
    fun setText_updatesTextAndCanSend() = runBlocking {
        repository.setText("Hello world")

        val state = repository.observeState().first()
        assertEquals("Hello world", state.text)
        assertTrue(state.canSend)
        assertFalse(state.isAttachButtonVisible)
    }

    @Test
    fun setReplyMessage_updatesReplyQuote() {
        repository.setReplyMessage(12345L, "Quoted text", 0)

        val state = repository.getState()
        assertNotNull(state.replyQuote)
        assertEquals(12345L, state.replyQuote?.messageId)
        assertEquals("Quoted text", state.replyQuote?.quote)
        assertFalse(state.replyQuote?.isEdit ?: true)
    }

    @Test
    fun setEditMessage_updatesTextAndEditQuote() {
        repository.setEditMessage(999L, "Original message")

        val state = repository.getState()
        assertEquals("Original message", state.text)
        assertNotNull(state.replyQuote)
        assertEquals(999L, state.replyQuote?.messageId)
        assertTrue(state.replyQuote?.isEdit ?: false)
    }

    @Test
    fun clearReplyOrEdit_removesReply() {
        repository.setReplyMessage(123L)
        assertNotNull(repository.getState().replyQuote)

        repository.clearReplyOrEdit()
        assertNull(repository.getState().replyQuote)
    }

    @Test
    fun recordingLifecycle_transitionsCorrectly() {
        // Start
        repository.startRecording(RecordType.VOICE)
        var state = repository.getState()
        assertEquals(RecordStatus.RECORDING, state.recordState.status)
        assertEquals(RecordType.VOICE, state.recordState.type)

        // Progress
        repository.updateRecordProgress(1500L, 0.75f)
        state = repository.getState()
        assertEquals(1500L, state.recordState.durationMs)
        assertEquals(0.75f, state.recordState.amplitude)

        // Lock
        repository.lockRecording()
        assertEquals(RecordStatus.LOCKED, repository.getState().recordState.status)

        // Pause & Resume
        repository.pauseRecording()
        assertEquals(RecordStatus.PAUSED, repository.getState().recordState.status)
        repository.resumeRecording()
        assertEquals(RecordStatus.LOCKED, repository.getState().recordState.status)

        // Stop -> Preview
        repository.stopRecording()
        assertEquals(RecordStatus.PREVIEW, repository.getState().recordState.status)

        // Cancel -> Idle
        repository.cancelRecording()
        assertEquals(RecordStatus.IDLE, repository.getState().recordState.status)
    }

    @Test
    fun toggleVoiceOnce_togglesFlagAndTtl() {
        repository.toggleVoiceOnce()
        var state = repository.getState()
        assertTrue(state.recordState.isVoiceOnce)
        assertEquals(1, state.sendOptions.ttlSeconds)

        repository.toggleVoiceOnce()
        state = repository.getState()
        assertFalse(state.recordState.isVoiceOnce)
        assertEquals(0, state.sendOptions.ttlSeconds)
    }

    @Test
    fun reset_restoresDefaultState() {
        repository.setText("Temporary draft")
        repository.setPanelMode(EnterViewPanelMode.EMOJI_STICKER)

        repository.reset()
        val state = repository.getState()
        assertEquals("", state.text)
        assertEquals(EnterViewPanelMode.NONE, state.panelMode)
    }
}
