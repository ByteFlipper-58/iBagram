package org.telegram.messenger.feature.chatinput

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.chatinput.data.mapper.ChatInputMapper
import org.telegram.messenger.feature.chatinput.data.repository.LegacyChatInputRepository
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputRecordState
import org.telegram.messenger.feature.chatinput.domain.model.ChatInputReplyQuote
import org.telegram.messenger.feature.chatinput.domain.model.EnterViewPanelMode
import org.telegram.messenger.feature.chatinput.domain.model.RecordStatus
import org.telegram.messenger.feature.chatinput.domain.model.RecordType
import org.telegram.messenger.feature.chatinput.domain.model.TextFormatStyle
import org.telegram.messenger.feature.chatinput.domain.usecase.CalculateSendButtonStateUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.FormatTextSelectionUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ValidateVoiceRecordActionUseCase
import org.telegram.messenger.feature.chatinput.presentation.ChatInputEvent
import org.telegram.messenger.feature.chatinput.presentation.ChatInputViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatInputDomainTest {

    @Test
    fun testSendButtonStateCalculations() {
        val useCase = CalculateSendButtonStateUseCase()

        // 1. Empty text, no reply, idle record -> record button, attach button visible, cannot send
        val emptyState = useCase(
            text = "",
            replyQuote = null,
            recordState = ChatInputRecordState()
        )
        assertFalse(emptyState.isSendButton)
        assertTrue(emptyState.isAttachButtonVisible)
        assertFalse(emptyState.canSend)

        // 2. Has text -> send button, attach hidden, can send
        val textState = useCase(
            text = "Hello world",
            replyQuote = null,
            recordState = ChatInputRecordState()
        )
        assertTrue(textState.isSendButton)
        assertFalse(textState.isAttachButtonVisible)
        assertTrue(textState.canSend)

        // 3. Empty text, but editing an existing message -> send button, can send
        val editState = useCase(
            text = "",
            replyQuote = ChatInputReplyQuote(messageId = 42L, isEdit = true),
            recordState = ChatInputRecordState()
        )
        assertTrue(editState.isSendButton)
        assertTrue(editState.canSend)

        // 4. Voice recording active -> send button, attach hidden
        val recordingState = useCase(
            text = "",
            replyQuote = null,
            recordState = ChatInputRecordState(status = RecordStatus.RECORDING)
        )
        assertTrue(recordingState.isSendButton)
        assertFalse(recordingState.isAttachButtonVisible)
        assertTrue(recordingState.canSend)
    }

    @Test
    fun testFormatTextSelection() {
        val formatter = FormatTextSelectionUseCase()
        val original = "Hello brave world"
        // "brave" is at [6, 11)

        // Bold
        val bold = formatter(original, 6, 11, TextFormatStyle.BOLD)
        assertEquals("Hello **brave** world", bold.newText)
        assertEquals(8, bold.newSelectionStart)
        assertEquals(13, bold.newSelectionEnd)

        // Italic
        val italic = formatter(original, 6, 11, TextFormatStyle.ITALIC)
        assertEquals("Hello __brave__ world", italic.newText)

        // Monospace
        val mono = formatter(original, 6, 11, TextFormatStyle.MONO)
        assertEquals("Hello `brave` world", mono.newText)

        // Spoiler
        val spoiler = formatter(original, 6, 11, TextFormatStyle.SPOILER)
        assertEquals("Hello ||brave|| world", spoiler.newText)

        // Quote
        val quote = formatter("First line", 0, 10, TextFormatStyle.QUOTE)
        assertEquals("> First line", quote.newText)
    }

    @Test
    fun testVoiceRecordValidation() {
        val validator = ValidateVoiceRecordActionUseCase()

        // Chat restrictions
        val restricted = validator(
            canRecordMedia = false,
            currentStatus = RecordStatus.IDLE,
            hasText = false
        )
        assertTrue(restricted is ValidateVoiceRecordActionUseCase.RecordValidation.Denied)

        // Presence of draft text blocks starting recording
        val hasDraft = validator(
            canRecordMedia = true,
            currentStatus = RecordStatus.IDLE,
            hasText = true
        )
        assertTrue(hasDraft is ValidateVoiceRecordActionUseCase.RecordValidation.Denied)

        // Normal allowed
        val allowed = validator(
            canRecordMedia = true,
            currentStatus = RecordStatus.IDLE,
            hasText = false
        )
        assertTrue(allowed is ValidateVoiceRecordActionUseCase.RecordValidation.Allowed)
    }

    @Test
    fun testMapperAndFormatting() {
        // Duration formatting
        assertEquals("00:00", ChatInputMapper.formatRecordDuration(0L))
        assertEquals("00:05", ChatInputMapper.formatRecordDuration(5400L))
        assertEquals("01:15", ChatInputMapper.formatRecordDuration(75000L))

        // Record type conversion
        assertEquals(0, ChatInputMapper.mapRecordTypeToInt(RecordType.VOICE))
        assertEquals(1, ChatInputMapper.mapRecordTypeToInt(RecordType.ROUND_VIDEO))
        assertEquals(RecordType.VOICE, ChatInputMapper.mapIntToRecordType(0))
        assertEquals(RecordType.ROUND_VIDEO, ChatInputMapper.mapIntToRecordType(1))

        // Panel and status names
        assertEquals("Keyboard", ChatInputMapper.getPanelName(EnterViewPanelMode.KEYBOARD))
        assertEquals("Emoji & Stickers", ChatInputMapper.getPanelName(EnterViewPanelMode.EMOJI_STICKER))
        assertEquals("Recording", ChatInputMapper.getRecordStatusName(RecordStatus.RECORDING))
        assertEquals("Locked", ChatInputMapper.getRecordStatusName(RecordStatus.LOCKED))
    }

    @Test
    fun testChatInputViewModelMviLifecycle() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val repository = LegacyChatInputRepository()
        val viewModel = ChatInputViewModel(repository = repository, scope = testScope)

        // 1. Initial State
        assertEquals("", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.canSend)
        assertTrue(viewModel.uiState.value.isAttachButtonVisible)
        assertEquals(EnterViewPanelMode.NONE, viewModel.uiState.value.panelMode)

        // 2. Typing text
        viewModel.onEvent(ChatInputEvent.TextChanged("Hello world", 0, 5))
        assertTrue(viewModel.uiState.value.canSend)
        assertFalse(viewModel.uiState.value.isAttachButtonVisible)
        assertEquals("Hello world", viewModel.uiState.value.text)

        // 3. Formatting selected "Hello" [0, 5) with BOLD
        viewModel.onEvent(ChatInputEvent.ApplyTextFormat(TextFormatStyle.BOLD))
        assertEquals("**Hello** world", viewModel.uiState.value.text)

        // 4. Toggle Panel
        viewModel.onEvent(ChatInputEvent.TogglePanel(EnterViewPanelMode.EMOJI_STICKER))
        assertEquals(EnterViewPanelMode.EMOJI_STICKER, viewModel.uiState.value.panelMode)
        // Toggle again to dismiss
        viewModel.onEvent(ChatInputEvent.TogglePanel(EnterViewPanelMode.EMOJI_STICKER))
        assertEquals(EnterViewPanelMode.NONE, viewModel.uiState.value.panelMode)

        // 5. Clear text and start voice recording
        viewModel.onEvent(ChatInputEvent.TextChanged(""))
        assertFalse(viewModel.uiState.value.canSend)

        viewModel.onEvent(ChatInputEvent.StartRecording(RecordType.VOICE))
        assertTrue(viewModel.uiState.value.isRecording)
        assertEquals(RecordStatus.RECORDING, viewModel.uiState.value.recordState.status)
        assertTrue(viewModel.uiState.value.canSend)

        // 6. Lock and progress update
        viewModel.onEvent(ChatInputEvent.LockRecording)
        assertEquals(RecordStatus.LOCKED, viewModel.uiState.value.recordState.status)

        viewModel.onEvent(ChatInputEvent.RecordProgressUpdated(durationMs = 3500L, amplitude = 0.8f))
        assertEquals("00:03", viewModel.uiState.value.formattedRecordDuration)

        // 7. Toggle Voice Once (View Once)
        viewModel.onEvent(ChatInputEvent.ToggleVoiceOnce)
        assertTrue(viewModel.uiState.value.recordState.isVoiceOnce)
        assertEquals(1, viewModel.uiState.value.sendOptions.ttlSeconds)

        // 8. Cancel Recording
        viewModel.onEvent(ChatInputEvent.CancelRecording)
        assertFalse(viewModel.uiState.value.isRecording)
        assertEquals(RecordStatus.IDLE, viewModel.uiState.value.recordState.status)

        viewModel.clear()
    }
}
