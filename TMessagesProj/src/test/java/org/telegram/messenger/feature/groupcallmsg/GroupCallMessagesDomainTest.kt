package org.telegram.messenger.feature.groupcallmsg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessageModel
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessageSendStatus
import org.telegram.messenger.feature.groupcallmsg.domain.model.GroupCallMessagesStateModel
import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ClearGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.GetGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ObserveGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.PopGroupCallMessageUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.SendGroupCallMessageUseCase
import org.telegram.messenger.feature.groupcallmsg.presentation.GroupCallMessagesEvent
import org.telegram.messenger.feature.groupcallmsg.presentation.GroupCallMessagesViewModel
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class GroupCallMessagesDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- 1. Domain Model Tests ---

    @Test
    fun `GroupCallMessageModel handles delivery status flags and reactions`() {
        val msgSending = GroupCallMessageModel(
            randomId = 101L,
            fromId = 555L,
            text = "Hello Conference!",
            isOut = true,
            sendStatus = GroupCallMessageSendStatus.SENDING,
            reactionAnimatedEmojiId = 999L
        )

        assertTrue(msgSending.isPending)
        assertFalse(msgSending.isSuccessful)
        assertFalse(msgSending.isFailed)
        assertEquals(999L, msgSending.reactionAnimatedEmojiId)

        val msgConfirmed = msgSending.copy(sendStatus = GroupCallMessageSendStatus.CONFIRMED)
        assertFalse(msgConfirmed.isPending)
        assertTrue(msgConfirmed.isSuccessful)
        assertFalse(msgConfirmed.isFailed)

        val msgError = msgSending.copy(sendStatus = GroupCallMessageSendStatus.ERROR)
        assertFalse(msgError.isPending)
        assertFalse(msgError.isSuccessful)
        assertTrue(msgError.isFailed)
    }

    @Test
    fun `GroupCallMessagesStateModel calculates count and hasMessages`() {
        val emptyState = GroupCallMessagesStateModel(callId = 123L)
        assertEquals(0, emptyState.activeMessagesCount)
        assertFalse(emptyState.hasMessages)

        val populatedState = GroupCallMessagesStateModel(
            callId = 123L,
            messages = listOf(
                GroupCallMessageModel(randomId = 1L, fromId = 10L, text = "Msg 1"),
                GroupCallMessageModel(randomId = 2L, fromId = 20L, text = "Msg 2")
            )
        )
        assertEquals(2, populatedState.activeMessagesCount)
        assertTrue(populatedState.hasMessages)
    }

    // --- 2. Fake Repository & Use Cases Tests ---

    private class FakeGroupCallMessagesRepository : GroupCallMessagesRepository {
        private val messagesMap = ConcurrentHashMap<Long, MutableList<GroupCallMessageModel>>()
        private val flows = ConcurrentHashMap<Long, MutableStateFlow<GroupCallMessagesStateModel>>()

        private fun getOrCreateFlow(callId: Long): MutableStateFlow<GroupCallMessagesStateModel> {
            return flows.computeIfAbsent(callId) {
                MutableStateFlow(GroupCallMessagesStateModel(callId = callId, messages = messagesMap[callId] ?: emptyList()))
            }
        }

        private fun updateFlow(callId: Long) {
            val list = messagesMap[callId] ?: emptyList()
            getOrCreateFlow(callId).value = GroupCallMessagesStateModel(callId = callId, messages = list.toList())
        }

        override fun observeCallMessages(callId: Long): Flow<GroupCallMessagesStateModel> {
            return getOrCreateFlow(callId)
        }

        override fun getCallMessages(callId: Long): GroupCallMessagesStateModel {
            val list = messagesMap[callId] ?: emptyList()
            return GroupCallMessagesStateModel(callId = callId, messages = list.toList())
        }

        override fun sendCallMessage(callId: Long, sendAsPeerId: Long, text: String): Boolean {
            val list = messagesMap.computeIfAbsent(callId) { mutableListOf() }
            val newMsg = GroupCallMessageModel(
                randomId = System.currentTimeMillis(),
                fromId = sendAsPeerId,
                text = text,
                isOut = true,
                sendStatus = GroupCallMessageSendStatus.CONFIRMED
            )
            list.add(newMsg)
            updateFlow(callId)
            return true
        }

        override fun popMessage(callId: Long) {
            val list = messagesMap[callId]
            if (!list.isNullOrEmpty()) {
                list.removeAt(0)
                updateFlow(callId)
            }
        }

        override fun clearCallMessages(callId: Long) {
            messagesMap.remove(callId)
            updateFlow(callId)
        }
    }

    @Test
    fun `Use cases send, observe, pop and clear group call messages`() = runTest {
        val fakeRepo = FakeGroupCallMessagesRepository()
        val observeUseCase = ObserveGroupCallMessagesUseCase(fakeRepo)
        val getUseCase = GetGroupCallMessagesUseCase(fakeRepo)
        val sendUseCase = SendGroupCallMessageUseCase(fakeRepo)
        val popUseCase = PopGroupCallMessageUseCase(fakeRepo)
        val clearUseCase = ClearGroupCallMessagesUseCase(fakeRepo)

        val callId = 777L
        val peerId = 12345L

        // Initial state empty
        val initialState = getUseCase(callId)
        assertFalse(initialState.hasMessages)

        // Send messages
        val send1 = sendUseCase(callId, peerId, "Test message 1")
        val send2 = sendUseCase(callId, peerId, "Test message 2")
        assertTrue(send1)
        assertTrue(send2)

        val updatedState = getUseCase(callId)
        assertEquals(2, updatedState.activeMessagesCount)
        assertEquals("Test message 1", updatedState.messages[0].text)
        assertEquals("Test message 2", updatedState.messages[1].text)

        // Pop one expired message
        popUseCase(callId)
        val afterPop = getUseCase(callId)
        assertEquals(1, afterPop.activeMessagesCount)
        assertEquals("Test message 2", afterPop.messages[0].text)

        // Clear
        clearUseCase(callId)
        val afterClear = getUseCase(callId)
        assertEquals(0, afterClear.activeMessagesCount)

        val flowState = observeUseCase(callId).first()
        assertEquals(0, flowState.activeMessagesCount)
    }

    // --- 3. ViewModel Tests ---

    @Test
    fun `GroupCallMessagesViewModel manages call state and handles events`() = runTest {
        val fakeRepo = FakeGroupCallMessagesRepository()
        val viewModel = GroupCallMessagesViewModel(
            observeGroupCallMessagesUseCase = ObserveGroupCallMessagesUseCase(fakeRepo),
            getGroupCallMessagesUseCase = GetGroupCallMessagesUseCase(fakeRepo),
            sendGroupCallMessageUseCase = SendGroupCallMessageUseCase(fakeRepo),
            popGroupCallMessageUseCase = PopGroupCallMessageUseCase(fakeRepo),
            clearGroupCallMessagesUseCase = ClearGroupCallMessagesUseCase(fakeRepo),
            initialCallId = 555L
        )

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasMessages)

        // Send message event
        viewModel.onEvent(GroupCallMessagesEvent.SendMessage(text = "Hello in call", sendAsPeerId = 100L))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasMessages)
        assertEquals(1, viewModel.uiState.value.messagesCount)
        assertEquals("Hello in call", viewModel.uiState.value.state.messages[0].text)

        // Pop message event
        viewModel.onEvent(GroupCallMessagesEvent.PopMessage)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.messagesCount)

        // Switch call id
        viewModel.onEvent(GroupCallMessagesEvent.SetCallId(888L))
        advanceUntilIdle()
        assertEquals(888L, viewModel.uiState.value.state.callId)

        // Clear messages
        viewModel.onEvent(GroupCallMessagesEvent.SendMessage("Another msg", 100L))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.messagesCount)
        viewModel.onEvent(GroupCallMessagesEvent.ClearMessages)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.messagesCount)

        // Dismiss error
        viewModel.onEvent(GroupCallMessagesEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // --- 4. DI Container Test ---

    @Test
    fun `AccountFeatureContainer resolves group call messages dependencies`() {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeGroupCallMessagesRepository()
        container.groupCallMessagesRepository = fakeRepo

        assertNotNull(container.groupCallMessagesRepository)
        assertNotNull(container.observeGroupCallMessagesUseCase)
        assertNotNull(container.getGroupCallMessagesUseCase)
        assertNotNull(container.sendGroupCallMessageUseCase)
        assertNotNull(container.popGroupCallMessageUseCase)
        assertNotNull(container.clearGroupCallMessagesUseCase)

        val vm = container.createGroupCallMessagesViewModel(999L)
        assertNotNull(vm)
    }
}
