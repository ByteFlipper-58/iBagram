package org.telegram.messenger.feature.botkeyboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.BotInlineKeyboard
import org.telegram.messenger.feature.botkeyboard.data.mapper.BotKeyboardMapper
import org.telegram.messenger.feature.botkeyboard.data.repository.LegacyBotKeyboardRepository
import org.telegram.messenger.feature.botkeyboard.domain.model.BotButtonColor
import org.telegram.messenger.feature.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.botkeyboard.domain.model.BotCustomButtonType
import org.telegram.messenger.feature.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.botkeyboard.domain.usecase.BuildBotKeyboardLayoutUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.CheckIsButtonWebViewUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.CheckIsForceReplyUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.ClearAllKeyboardsUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.GetKeyboardForMessageUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.ObserveBotKeyboardStateUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.RecordButtonPressedUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.RemoveKeyboardForMessageUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.ResolveCustomButtonTypeUseCase
import org.telegram.messenger.feature.botkeyboard.domain.usecase.SetKeyboardForMessageUseCase
import org.telegram.messenger.feature.botkeyboard.presentation.BotKeyboardEvent
import org.telegram.messenger.feature.botkeyboard.presentation.BotKeyboardViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class BotKeyboardDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBuildBotKeyboardLayoutUseCase() {
        val buildUseCase = BuildBotKeyboardLayoutUseCase()

        val btnDecline = BotButtonItem.CustomAction(
            customType = BotCustomButtonType.SUGGESTION_DECLINE,
            text = "Decline",
            color = BotButtonColor.DANGER
        )
        val btnAccept = BotButtonItem.CustomAction(
            customType = BotCustomButtonType.SUGGESTION_ACCEPT,
            text = "Accept",
            color = BotButtonColor.SUCCESS
        )
        val btnEdit = BotButtonItem.CustomAction(
            customType = BotCustomButtonType.SUGGESTION_EDIT,
            text = "Edit",
            color = BotButtonColor.PRIMARY
        )

        val row0 = listOf(btnDecline, btnAccept)
        val row1 = listOf(btnEdit)

        // Separator on row 1 (bit 1: 1 shl 1 = 2)
        val layout = buildUseCase(listOf(row0, row1), separators = 2)

        assertFalse(layout.isEmpty)
        assertEquals(2, layout.rows.size)
        assertEquals(3, layout.totalButtonsCount)
        assertFalse(layout.rows[0].hasSeparator)
        assertTrue(layout.rows[1].hasSeparator)
    }

    @Test
    fun testResolveCustomButtonType() {
        val resolveUseCase = ResolveCustomButtonTypeUseCase()

        assertEquals(BotCustomButtonType.SUGGESTION_DECLINE, resolveUseCase(1))
        assertEquals(BotCustomButtonType.SUGGESTION_ACCEPT, resolveUseCase(2))
        assertEquals(BotCustomButtonType.SUGGESTION_EDIT, resolveUseCase(3))
        assertEquals(BotCustomButtonType.OPEN_MESSAGE_THREAD, resolveUseCase(4))
        assertEquals(BotCustomButtonType.GIFT_OFFER_DECLINE, resolveUseCase(5))
        assertEquals(BotCustomButtonType.GIFT_OFFER_ACCEPT, resolveUseCase(6))
        assertEquals(BotCustomButtonType.SHARING_OFFER_DECLINE, resolveUseCase(7))
        assertEquals(BotCustomButtonType.SHARING_OFFER_ACCEPT, resolveUseCase(8))
        assertNull(resolveUseCase(999))
    }

    @Test
    fun testForceReplyAndWebViewChecks() {
        val repository = LegacyBotKeyboardRepository(0)
        val checkForceReply = CheckIsForceReplyUseCase(repository)
        val checkWebView = CheckIsButtonWebViewUseCase(repository)

        val forceReply = TLRPC.TL_replyKeyboardForceReply()
        assertTrue(checkForceReply(forceReply))

        val normalMarkup = TLRPC.TL_replyKeyboardMarkup()
        normalMarkup.force_reply = false
        assertFalse(checkForceReply(normalMarkup))

        assertFalse(checkForceReply(null))
        assertFalse(checkWebView(null))
    }

    @Test
    fun testBotKeyboardRepositoryOperations() {
        val repository = LegacyBotKeyboardRepository(0)
        val setKeyboard = SetKeyboardForMessageUseCase(repository)
        val getKeyboard = GetKeyboardForMessageUseCase(repository)
        val removeKeyboard = RemoveKeyboardForMessageUseCase(repository)
        val clearAll = ClearAllKeyboardsUseCase(repository)
        val recordPressed = RecordButtonPressedUseCase(repository)

        val messageId = 12345L
        val button = BotButtonItem.BotAction(text = "Click me", color = BotButtonColor.PRIMARY)
        val buildUseCase = BuildBotKeyboardLayoutUseCase()
        val layout = buildUseCase(listOf(listOf(button)))

        assertNull(getKeyboard(messageId))

        setKeyboard(messageId, layout)
        val retrieved = getKeyboard(messageId)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.totalButtonsCount)

        recordPressed(button)
        assertEquals(button, repository.getCurrentState().lastPressedButton)

        val removed = removeKeyboard(messageId)
        assertNotNull(removed)
        assertNull(getKeyboard(messageId))

        setKeyboard(100L, layout)
        setKeyboard(200L, layout)
        assertEquals(2, repository.getCurrentState().activeKeyboards.size)

        clearAll()
        assertEquals(0, repository.getCurrentState().activeKeyboards.size)
    }

    @Test
    fun testBotKeyboardMapper() {
        assertEquals(BotButtonColor.PRIMARY, BotKeyboardMapper.mapColor(BotInlineKeyboard.BackgroundColor.PRIMARY))
        assertEquals(BotButtonColor.SUCCESS, BotKeyboardMapper.mapColor(BotInlineKeyboard.BackgroundColor.SUCCESS))
        assertEquals(BotButtonColor.DANGER, BotKeyboardMapper.mapColor(BotInlineKeyboard.BackgroundColor.DANGER))
        assertEquals(BotButtonColor.NONE, BotKeyboardMapper.mapColor(BotInlineKeyboard.BackgroundColor.NONE))
        assertEquals(BotButtonColor.NONE, BotKeyboardMapper.mapColor(null))

        assertNull(BotKeyboardMapper.mapButton(null))
        val emptyLayout = BotKeyboardMapper.mapSource(null)
        assertTrue(emptyLayout.isEmpty)
    }

    @Test
    fun testBotKeyboardViewModelFlow() = runTest(testDispatcher) {
        val repository = LegacyBotKeyboardRepository(0)
        val getKeyboard = GetKeyboardForMessageUseCase(repository)
        val setKeyboard = SetKeyboardForMessageUseCase(repository)
        val removeKeyboard = RemoveKeyboardForMessageUseCase(repository)
        val clearAll = ClearAllKeyboardsUseCase(repository)
        val recordPressed = RecordButtonPressedUseCase(repository)
        val observeState = ObserveBotKeyboardStateUseCase(repository)

        val viewModel = BotKeyboardViewModel(
            getKeyboardUseCase = getKeyboard,
            setKeyboardUseCase = setKeyboard,
            removeKeyboardUseCase = removeKeyboard,
            clearAllKeyboardsUseCase = clearAll,
            recordButtonPressedUseCase = recordPressed,
            observeStateUseCase = observeState
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.currentLayout)
        assertEquals(0, viewModel.uiState.value.totalKeyboardsCount)

        val messageId = 777L
        val button = BotButtonItem.CustomAction(
            customType = BotCustomButtonType.OPEN_MESSAGE_THREAD,
            text = "Open Thread",
            color = BotButtonColor.NONE
        )
        val layout = BuildBotKeyboardLayoutUseCase()(listOf(listOf(button)))

        // 1. Select message
        viewModel.onEvent(BotKeyboardEvent.SelectMessage(messageId))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(messageId, viewModel.uiState.value.currentMessageId)
        assertNull(viewModel.uiState.value.currentLayout)

        // 2. Set layout for selected message
        viewModel.onEvent(BotKeyboardEvent.SetLayout(messageId, layout))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.currentLayout)
        assertEquals(1, viewModel.uiState.value.currentLayout?.totalButtonsCount)
        assertEquals(1, viewModel.uiState.value.totalKeyboardsCount)

        // 3. Press button
        viewModel.onEvent(BotKeyboardEvent.PressButton(messageId, button))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(button, viewModel.uiState.value.lastPressedButton)

        // 4. Remove layout
        viewModel.onEvent(BotKeyboardEvent.RemoveLayout(messageId))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.currentLayout)
        assertEquals(0, viewModel.uiState.value.totalKeyboardsCount)
    }
}
