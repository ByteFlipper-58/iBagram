package org.telegram.messenger.feature.ephemeralmessages

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
import org.telegram.messenger.feature.ephemeralmessages.data.mapper.EphemeralMessagesMapper
import org.telegram.messenger.feature.ephemeralmessages.data.repository.LegacyEphemeralMessagesRepository
import org.telegram.messenger.feature.ephemeralmessages.domain.model.EphemeralMessageIdHelper
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ClearAllWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.GetEphemeralCommandBotIdUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.GetEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.GetWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.IsEphemeralCommandUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.IsEphemeralMessageIdUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ObserveEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.PackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.ParseBotCommandUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.PutWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.RemoveWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.ephemeralmessages.domain.usecase.UnpackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.ephemeralmessages.presentation.EphemeralMessagesEvent
import org.telegram.messenger.feature.ephemeralmessages.presentation.EphemeralMessagesViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_ephemeral

@OptIn(ExperimentalCoroutinesApi::class)
class EphemeralMessagesDomainTest {

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
    fun testParseBotCommandUseCase() {
        val parseUseCase = ParseBotCommandUseCase()

        val startCmd = parseUseCase("/start")
        assertNotNull(startCmd)
        assertEquals("start", startCmd?.command)
        assertNull(startCmd?.botUsername)

        val botCmd = parseUseCase("/help@mybot with args")
        assertNotNull(botCmd)
        assertEquals("help", botCmd?.command)
        assertEquals("mybot", botCmd?.botUsername)

        assertNull(parseUseCase("regular message"))
        assertNull(parseUseCase("/"))
        assertNull(parseUseCase(""))
    }

    @Test
    fun testEphemeralMessageIdPackAndUnpack() {
        val packUseCase = PackEphemeralMessageIdUseCase()
        val unpackUseCase = UnpackEphemeralMessageIdUseCase()
        val isEphemeralUseCase = IsEphemeralMessageIdUseCase()

        val originalId = 12345
        assertFalse(isEphemeralUseCase(originalId))

        val packed = packUseCase(originalId)
        assertTrue(isEphemeralUseCase(packed))

        val unpacked = unpackUseCase(packed)
        assertEquals(originalId, unpacked)
    }

    @Test
    fun testEphemeralBotCommandsRegistry() {
        val repository = LegacyEphemeralMessagesRepository(0)
        val isEphemeral = IsEphemeralCommandUseCase(repository)
        val getBotId = GetEphemeralCommandBotIdUseCase(repository)

        val dialogId = -100123L

        // Register ephemeral command
        repository.registerTestBotCommand("eph", null, 777L, isEphemeral = true)
        assertTrue(isEphemeral("/eph", dialogId))
        assertEquals(777L, getBotId("/eph", dialogId))

        // Register non-ephemeral command
        repository.registerTestBotCommand("normal", "somebot", 888L, isEphemeral = false)
        assertFalse(isEphemeral("/normal@somebot", dialogId))
        assertEquals(0L, getBotId("/normal@somebot", dialogId))

        // Unknown command
        assertFalse(isEphemeral("/unknown", dialogId))
    }

    @Test
    fun testAnchorBindings() {
        val repository = LegacyEphemeralMessagesRepository(0)
        val putAnchor = PutWelcomeAnchorBindingUseCase(repository)
        val removeAnchor = RemoveWelcomeAnchorBindingUseCase(repository)
        val getAnchors = GetWelcomeAnchorBindingsUseCase(repository)
        val clearAll = ClearAllWelcomeAnchorBindingsUseCase(repository)

        val dialogId = -100456L

        putAnchor(dialogId, 10, 1001)
        putAnchor(dialogId, 20, 1002)

        val bindings = getAnchors(dialogId)
        assertEquals(2, bindings.size)
        assertEquals(1001, bindings[10])
        assertEquals(1002, bindings[20])

        removeAnchor(dialogId, 10, 1001)
        val updated = getAnchors(dialogId)
        assertEquals(1, updated.size)
        assertNull(updated[10])
        assertEquals(1002, updated[20])

        clearAll()
        assertEquals(0, getAnchors(dialogId).size)
    }

    @Test
    fun testEphemeralMessagesMapper() {
        val ephemeralMsg = TL_ephemeral.TL_ephemeralMessage()
        ephemeralMsg.id = 555
        val peerChannel = TLRPC.TL_peerChannel()
        peerChannel.channel_id = 999
        ephemeralMsg.peer_id = peerChannel
        val fromUser = TLRPC.TL_peerUser()
        fromUser.user_id = 111
        ephemeralMsg.from_id = fromUser
        ephemeralMsg.receiver_id = 222
        ephemeralMsg.message = "Ephemeral secret"
        ephemeralMsg.welcome = true
        ephemeralMsg.anchor_msg_id = 333

        val domainItem = EphemeralMessagesMapper.toDomain(ephemeralMsg)
        assertEquals(555, domainItem.id)
        assertEquals(-999L, domainItem.dialogId)
        assertEquals(111L, domainItem.fromId)
        assertEquals(222L, domainItem.receiverBotId)
        assertEquals("Ephemeral secret", domainItem.message)
        assertTrue(domainItem.isWelcome)
        assertEquals(333, domainItem.anchorMsgId)

        // Test TLRPC.Message conversion
        val message = TLRPC.TL_message()
        message.id = EphemeralMessageIdHelper.pack(777)
        val peerUser = TLRPC.TL_peerUser()
        peerUser.user_id = 444
        message.peer_id = peerUser
        message.ephemeralReceiverBotId = -1L
        message.ephemeralAnchorMsgId = 888
        message.message = "Fake default message"

        val convertedDomain = EphemeralMessagesMapper.toDomain(message)
        assertEquals(777, convertedDomain.id)
        assertEquals(444L, convertedDomain.dialogId)
        assertTrue(convertedDomain.isWelcome)
        assertEquals(888, convertedDomain.anchorMsgId)
        assertEquals("Fake default message", convertedDomain.message)
    }

    @Test
    fun testEphemeralMessagesViewModelFlow() = runTest(testDispatcher) {
        val repository = LegacyEphemeralMessagesRepository(0)
        val parseUseCase = ParseBotCommandUseCase()
        val isEphemeralUseCase = IsEphemeralCommandUseCase(repository)
        val putAnchorUseCase = PutWelcomeAnchorBindingUseCase(repository)
        val removeAnchorUseCase = RemoveWelcomeAnchorBindingUseCase(repository)
        val getAnchorsUseCase = GetWelcomeAnchorBindingsUseCase(repository)
        val clearAllUseCase = ClearAllWelcomeAnchorBindingsUseCase(repository)
        val observeStateUseCase = ObserveEphemeralMessagesStateUseCase(repository)

        repository.registerTestBotCommand("secretcmd", null, 999L, isEphemeral = true)

        val viewModel = EphemeralMessagesViewModel(
            parseBotCommandUseCase = parseUseCase,
            isEphemeralCommandUseCase = isEphemeralUseCase,
            putWelcomeAnchorBindingUseCase = putAnchorUseCase,
            removeWelcomeAnchorBindingUseCase = removeAnchorUseCase,
            getWelcomeAnchorBindingsUseCase = getAnchorsUseCase,
            clearAllWelcomeAnchorBindingsUseCase = clearAllUseCase,
            observeStateUseCase = observeStateUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCurrentInputEphemeral)

        val dialogId = -100555L

        // 1. Regular text typed
        viewModel.onEvent(EphemeralMessagesEvent.InputTextChanged(dialogId, "Hello guys"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCurrentInputEphemeral)
        assertNull(viewModel.uiState.value.currentCommandInfo)

        // 2. Ephemeral command typed
        viewModel.onEvent(EphemeralMessagesEvent.InputTextChanged(dialogId, "/secretcmd param1"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isCurrentInputEphemeral)
        assertNotNull(viewModel.uiState.value.currentCommandInfo)
        assertEquals("secretcmd", viewModel.uiState.value.currentCommandInfo?.command)

        // 3. Anchor registration
        viewModel.onEvent(EphemeralMessagesEvent.RegisterAnchor(dialogId, 50, 5001))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(EphemeralMessagesEvent.SelectDialog(dialogId))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeAnchorBindings.size)
        assertEquals(5001, viewModel.uiState.value.activeAnchorBindings[50])

        // 4. Anchor unregistration
        viewModel.onEvent(EphemeralMessagesEvent.UnregisterAnchor(dialogId, 50, 5001))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.activeAnchorBindings.size)
    }
}
