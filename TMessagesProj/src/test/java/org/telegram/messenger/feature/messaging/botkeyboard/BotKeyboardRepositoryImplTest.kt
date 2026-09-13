package org.telegram.messenger.feature.messaging.botkeyboard

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
import org.telegram.messenger.feature.messaging.botkeyboard.data.datasource.BotKeyboardLocalDataSource
import org.telegram.messenger.feature.messaging.botkeyboard.data.datasource.BotKeyboardRemoteDataSource
import org.telegram.messenger.feature.messaging.botkeyboard.data.repository.BotKeyboardRepositoryImpl
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonColor
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonItem
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotButtonTypeCategory
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardLayout
import org.telegram.messenger.feature.messaging.botkeyboard.domain.model.BotKeyboardRow
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class BotKeyboardRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: BotKeyboardLocalDataSource
    private lateinit var remoteDataSource: BotKeyboardRemoteDataSource
    private lateinit var repository: BotKeyboardRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = BotKeyboardLocalDataSource(0)
        remoteDataSource = BotKeyboardRemoteDataSource(0)
        repository = BotKeyboardRepositoryImpl(
            account = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateEmpty() {
        val state = repository.getCurrentState()
        assertTrue(state.activeKeyboards.isEmpty())
        assertNull(state.lastPressedButton)
    }

    @Test
    fun testSetAndGetKeyboardForMessage() = runTest {
        val button = BotButtonItem.BotAction(
            text = "Click me",
            color = BotButtonColor.PRIMARY,
            category = BotButtonTypeCategory.CALLBACK
        )
        val row = BotKeyboardRow(buttons = listOf(button))
        val layout = BotKeyboardLayout(rows = listOf(row))

        repository.setKeyboardForMessage(12345L, layout)

        val retrieved = repository.getKeyboardForMessage(12345L)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.rows?.size)
        assertEquals("Click me", retrieved?.rows?.first()?.buttons?.first()?.text)
        assertEquals(1, repository.getCurrentState().activeKeyboards.size)
    }

    @Test
    fun testRemoveKeyboardForMessage() = runTest {
        val layout = BotKeyboardLayout(rows = listOf(BotKeyboardRow()))
        repository.setKeyboardForMessage(999L, layout)
        assertNotNull(repository.getKeyboardForMessage(999L))

        val removed = repository.removeKeyboardForMessage(999L)
        assertNotNull(removed)
        assertNull(repository.getKeyboardForMessage(999L))
        assertTrue(repository.getCurrentState().activeKeyboards.isEmpty())
    }

    @Test
    fun testClearAllKeyboards() = runTest {
        repository.setKeyboardForMessage(1L, BotKeyboardLayout())
        repository.setKeyboardForMessage(2L, BotKeyboardLayout())
        assertEquals(2, repository.getCurrentState().activeKeyboards.size)

        repository.clearAllKeyboards()
        assertTrue(repository.getCurrentState().activeKeyboards.isEmpty())
    }

    @Test
    fun testRecordButtonPressed() = runTest {
        val button = BotButtonItem.BotAction(
            text = "Action Button",
            color = BotButtonColor.SUCCESS
        )

        repository.recordButtonPressed(button)

        assertEquals(button, repository.getCurrentState().lastPressedButton)
    }

    @Test
    fun testIsForceReply() {
        assertFalse(repository.isForceReply(null))
        assertFalse(repository.isForceReply("invalid"))

        val forceReplyMarkup = TLRPC.TL_replyKeyboardForceReply()
        assertTrue(repository.isForceReply(forceReplyMarkup))
    }

    @Test
    fun testIsButtonWebView() {
        assertFalse(repository.isButtonWebView(null))
        assertFalse(repository.isButtonWebView("invalid"))
    }

    @Test
    fun testStranglerHooks() {
        val repoFromHelper = TLKeyboardHelper.getBotKeyboardRepository(0)
        assertNotNull(repoFromHelper)

        val repoFromInline = BotInlineKeyboard.getBotKeyboardRepository(0)
        assertNotNull(repoFromInline)
    }
}
