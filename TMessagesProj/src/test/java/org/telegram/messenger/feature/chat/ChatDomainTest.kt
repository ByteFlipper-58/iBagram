package org.telegram.messenger.feature.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chat.data.mapper.ChatMessageMapper
import org.telegram.messenger.feature.chat.domain.model.MessageDeliveryStatus
import org.telegram.messenger.feature.chat.domain.model.MessageModel
import org.telegram.messenger.feature.chat.domain.repository.ChatRepository
import org.telegram.messenger.feature.chat.domain.usecase.DeleteMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.GetMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.LoadHistoryUseCase
import org.telegram.messenger.feature.chat.domain.usecase.ObserveMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.SendMessageUseCase
import org.telegram.messenger.feature.chat.presentation.ChatEvent
import org.telegram.messenger.feature.chat.presentation.ChatUiState
import org.telegram.messenger.feature.chat.presentation.ChatViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeChatRepository : ChatRepository {
        val messagesFlow = MutableSharedFlow<List<MessageModel>>(replay = 1)
        val currentMessages = mutableListOf<MessageModel>()
        var sendSuccess = true
        var deleteSuccess = true
        var loadHistorySuccess = true

        override fun observeMessages(dialogId: Long): Flow<List<MessageModel>> = messagesFlow.asSharedFlow()

        override suspend fun getMessages(dialogId: Long): Result<List<MessageModel>> =
            Result.success(currentMessages.toList())

        override suspend fun loadHistory(dialogId: Long, count: Int): Result<Unit> {
            return if (loadHistorySuccess) {
                val olderMessage = MessageModel(
                    id = currentMessages.size + 1000,
                    dialogId = dialogId,
                    senderId = 999L,
                    text = "Older message",
                    date = 1600000000,
                    isOut = false,
                    isUnread = false
                )
                currentMessages.add(0, olderMessage)
                messagesFlow.emit(currentMessages.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to load history"))
            }
        }

        override suspend fun sendMessage(dialogId: Long, text: String): Result<Unit> {
            return if (sendSuccess) {
                val newMsg = MessageModel(
                    id = currentMessages.size + 1,
                    dialogId = dialogId,
                    senderId = 1L,
                    text = text,
                    date = 1700000000,
                    isOut = true,
                    isUnread = true,
                    status = MessageDeliveryStatus.SENT
                )
                currentMessages.add(newMsg)
                messagesFlow.emit(currentMessages.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Network failure"))
            }
        }

        override suspend fun deleteMessages(dialogId: Long, messageIds: List<Int>, revoke: Boolean): Result<Unit> {
            return if (deleteSuccess) {
                currentMessages.removeAll { it.id in messageIds }
                messagesFlow.emit(currentMessages.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Delete failed"))
            }
        }
    }

    @Test
    fun testChatMessageMapper() {
        val model = ChatMessageMapper.mapToDomain(
            id = 42,
            dialogId = 1001L,
            senderId = 555L,
            text = "Hello world",
            date = 1680000000,
            isOut = true,
            isUnread = false,
            status = MessageDeliveryStatus.SENT,
            replyToMsgId = 10
        )

        assertEquals(42, model.id)
        assertEquals(1001L, model.dialogId)
        assertEquals(555L, model.senderId)
        assertEquals("Hello world", model.text)
        assertEquals(1680000000, model.date)
        assertTrue(model.isOut)
        assertFalse(model.isUnread)
        assertEquals(MessageDeliveryStatus.SENT, model.status)
        assertEquals(10, model.replyToMsgId)
    }

    @Test
    fun testObserveMessagesUseCase() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg = MessageModel(id = 1, dialogId = 100L, senderId = 2L, text = "Test", date = 100, isOut = false, isUnread = false)
        repo.currentMessages.add(msg)
        repo.messagesFlow.emit(listOf(msg))

        val useCase = ObserveMessagesUseCase(repo)
        val result = useCase(100L).first()

        assertEquals(1, result.size)
        assertEquals("Test", result[0].text)
    }

    @Test
    fun testGetMessagesUseCase() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg = MessageModel(id = 1, dialogId = 100L, senderId = 2L, text = "Test", date = 100, isOut = false, isUnread = false)
        repo.currentMessages.add(msg)

        val useCase = GetMessagesUseCase(repo)
        val result = useCase(100L)

        assertTrue(result.isSuccess)
        assertEquals(1, (result as Result.Success).data.size)
    }

    @Test
    fun testSendMessageUseCase_success() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val useCase = SendMessageUseCase(repo)

        val result = useCase(200L, "Hello")

        assertTrue(result.isSuccess)
        assertEquals(1, repo.currentMessages.size)
        assertEquals("Hello", repo.currentMessages[0].text)
    }

    @Test
    fun testSendMessageUseCase_failure() = runTest(testDispatcher) {
        val repo = FakeChatRepository().apply { sendSuccess = false }
        val useCase = SendMessageUseCase(repo)

        val result = useCase(200L, "Hello")

        assertTrue(result.isFailure)
        assertEquals("Network failure", (result as Result.Failure).error.message)
    }

    @Test
    fun testDeleteMessagesUseCase() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg1 = MessageModel(id = 10, dialogId = 300L, senderId = 1L, text = "M1", date = 1, isOut = true, isUnread = false)
        val msg2 = MessageModel(id = 11, dialogId = 300L, senderId = 1L, text = "M2", date = 2, isOut = true, isUnread = false)
        repo.currentMessages.addAll(listOf(msg1, msg2))

        val useCase = DeleteMessagesUseCase(repo)
        val result = useCase(300L, listOf(10))

        assertTrue(result.isSuccess)
        assertEquals(1, repo.currentMessages.size)
        assertEquals(11, repo.currentMessages[0].id)
    }

    @Test
    fun testLoadHistoryUseCase() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val useCase = LoadHistoryUseCase(repo)

        val result = useCase(400L, 30)

        assertTrue(result.isSuccess)
        assertEquals(1, repo.currentMessages.size)
        assertEquals("Older message", repo.currentMessages[0].text)
    }

    @Test
    fun testChatViewModel_initialStateAndObserve() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg = MessageModel(id = 1, dialogId = 500L, senderId = 2L, text = "Hello chat", date = 100, isOut = false, isUnread = false)
        repo.currentMessages.add(msg)
        repo.messagesFlow.emit(listOf(msg))

        val viewModel = ChatViewModel(
            account = 0,
            dialogId = 500L,
            observeMessagesUseCase = ObserveMessagesUseCase(repo),
            loadHistoryUseCase = LoadHistoryUseCase(repo),
            sendMessageUseCase = SendMessageUseCase(repo),
            deleteMessagesUseCase = DeleteMessagesUseCase(repo)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChatUiState.Success)
        val success = state as ChatUiState.Success
        assertEquals(1, success.messages.size)
        assertEquals("Hello chat", success.messages[0].text)
        assertEquals(500L, success.dialogId)
        assertFalse(success.isSending)
    }

    @Test
    fun testChatViewModel_onSendMessage_success() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val viewModel = ChatViewModel(
            account = 0,
            dialogId = 600L,
            observeMessagesUseCase = ObserveMessagesUseCase(repo),
            loadHistoryUseCase = LoadHistoryUseCase(repo),
            sendMessageUseCase = SendMessageUseCase(repo),
            deleteMessagesUseCase = DeleteMessagesUseCase(repo)
        )

        advanceUntilIdle()

        val events = mutableListOf<ChatEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onSendMessage("New message")
        advanceUntilIdle()

        assertEquals(2, events.size)
        assertEquals(ChatEvent.MessageSent, events[0])
        assertEquals(ChatEvent.ScrollToBottom, events[1])

        val state = viewModel.uiState.value as ChatUiState.Success
        assertEquals(1, state.messages.size)
        assertEquals("New message", state.messages[0].text)
        assertFalse(state.isSending)

        job.cancel()
    }

    @Test
    fun testChatViewModel_onSendMessage_failure() = runTest(testDispatcher) {
        val repo = FakeChatRepository().apply { sendSuccess = false }
        val viewModel = ChatViewModel(
            account = 0,
            dialogId = 700L,
            observeMessagesUseCase = ObserveMessagesUseCase(repo),
            loadHistoryUseCase = LoadHistoryUseCase(repo),
            sendMessageUseCase = SendMessageUseCase(repo),
            deleteMessagesUseCase = DeleteMessagesUseCase(repo)
        )

        advanceUntilIdle()

        val events = mutableListOf<ChatEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onSendMessage("Failing message")
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is ChatEvent.ShowError)
        assertEquals("Network failure", (events[0] as ChatEvent.ShowError).message)

        job.cancel()
    }

    @Test
    fun testChatViewModel_onLoadHistory() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg = MessageModel(id = 1, dialogId = 800L, senderId = 2L, text = "Latest", date = 200, isOut = false, isUnread = false)
        repo.currentMessages.add(msg)
        repo.messagesFlow.emit(listOf(msg))

        val viewModel = ChatViewModel(
            account = 0,
            dialogId = 800L,
            observeMessagesUseCase = ObserveMessagesUseCase(repo),
            loadHistoryUseCase = LoadHistoryUseCase(repo),
            sendMessageUseCase = SendMessageUseCase(repo),
            deleteMessagesUseCase = DeleteMessagesUseCase(repo)
        )

        advanceUntilIdle()
        viewModel.onLoadHistory(30)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ChatUiState.Success
        assertEquals(2, state.messages.size)
        assertFalse(state.isLoadingHistory)
    }

    @Test
    fun testChatViewModel_onDeleteMessages() = runTest(testDispatcher) {
        val repo = FakeChatRepository()
        val msg = MessageModel(id = 50, dialogId = 900L, senderId = 1L, text = "To delete", date = 1, isOut = true, isUnread = false)
        repo.currentMessages.add(msg)
        repo.messagesFlow.emit(listOf(msg))

        val viewModel = ChatViewModel(
            account = 0,
            dialogId = 900L,
            observeMessagesUseCase = ObserveMessagesUseCase(repo),
            loadHistoryUseCase = LoadHistoryUseCase(repo),
            sendMessageUseCase = SendMessageUseCase(repo),
            deleteMessagesUseCase = DeleteMessagesUseCase(repo)
        )

        advanceUntilIdle()
        viewModel.onDeleteMessages(listOf(50))
        advanceUntilIdle()

        val state = viewModel.uiState.value as ChatUiState.Success
        assertEquals(0, state.messages.size)
    }
}
