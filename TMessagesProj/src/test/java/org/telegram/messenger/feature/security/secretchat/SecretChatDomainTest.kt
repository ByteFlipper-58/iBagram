package org.telegram.messenger.feature.security.secretchat

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.DialogObject
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.secretchat.data.mapper.SecretChatMapper
import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatModel
import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatState
import org.telegram.messenger.feature.security.secretchat.domain.repository.SecretChatRepository
import org.telegram.messenger.feature.security.secretchat.domain.usecase.AcceptSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.DeclineSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.GetSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.ObserveSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.ObserveSecretChatsUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.SendScreenshotNotificationUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.SetSecretChatTtlUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.StartSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.presentation.SecretChatEvent
import org.telegram.messenger.feature.security.secretchat.presentation.SecretChatUiState
import org.telegram.messenger.feature.security.secretchat.presentation.SecretChatViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class SecretChatDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSecretChatRepository : SecretChatRepository {
        val chatsMap = mutableMapOf<Int, SecretChatModel>()
        val chatsFlow = MutableSharedFlow<List<SecretChatModel>>(replay = 1)
        val singleChatFlows = mutableMapOf<Int, MutableSharedFlow<SecretChatModel?>>()
        var shouldSucceed = true

        private fun getSingleChatFlow(chatId: Int): MutableSharedFlow<SecretChatModel?> {
            return singleChatFlows.getOrPut(chatId) {
                MutableSharedFlow<SecretChatModel?>(replay = 1).apply {
                    tryEmit(chatsMap[chatId])
                }
            }
        }

        fun putChat(chat: SecretChatModel) {
            chatsMap[chat.chatId] = chat
            chatsFlow.tryEmit(chatsMap.values.toList())
            getSingleChatFlow(chat.chatId).tryEmit(chat)
        }

        override fun observeSecretChat(chatId: Int): Flow<SecretChatModel?> = getSingleChatFlow(chatId).asSharedFlow()

        override fun observeSecretChats(): Flow<List<SecretChatModel>> = chatsFlow.asSharedFlow()

        override suspend fun getSecretChat(chatId: Int): SecretChatModel? = chatsMap[chatId]

        override suspend fun startSecretChat(userId: Long): Result<Int> {
            return if (shouldSucceed) {
                val newChatId = (chatsMap.keys.maxOrNull() ?: 0) + 1
                val newChat = SecretChatModel(
                    chatId = newChatId,
                    dialogId = DialogObject.makeEncryptedDialogId(newChatId.toLong()),
                    userId = userId,
                    userName = "User $userId",
                    state = SecretChatState.WAITING,
                    isCreator = true
                )
                putChat(newChat)
                Result.success(newChatId)
            } else {
                Result.failure(AppError.Generic("Failed to start secret chat"))
            }
        }

        override suspend fun acceptSecretChat(chatId: Int): Result<Unit> {
            return if (shouldSucceed) {
                val chat = chatsMap[chatId]
                if (chat != null) {
                    putChat(chat.copy(state = SecretChatState.ACTIVE))
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Chat not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to accept chat"))
            }
        }

        override suspend fun declineSecretChat(chatId: Int): Result<Unit> {
            return if (shouldSucceed) {
                val chat = chatsMap[chatId]
                if (chat != null) {
                    putChat(chat.copy(state = SecretChatState.DISCARDED))
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Chat not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to decline chat"))
            }
        }

        override suspend fun setTtl(chatId: Int, ttlSeconds: Int): Result<Unit> {
            return if (shouldSucceed) {
                val chat = chatsMap[chatId]
                if (chat != null) {
                    putChat(chat.copy(ttlSeconds = ttlSeconds))
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Chat not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to set TTL"))
            }
        }

        override suspend fun sendScreenshotNotification(chatId: Int): Result<Unit> {
            return if (shouldSucceed) {
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to send screenshot notification"))
            }
        }
    }

    @Test
    fun testSecretChatMapperStates() {
        val waiting = TLRPC.TL_encryptedChatWaiting()
        val requested = TLRPC.TL_encryptedChatRequested()
        val active = TLRPC.TL_encryptedChat()
        val discarded = TLRPC.TL_encryptedChatDiscarded()

        assertEquals(SecretChatState.WAITING, SecretChatMapper.mapState(waiting))
        assertEquals(SecretChatState.REQUESTED, SecretChatMapper.mapState(requested))
        assertEquals(SecretChatState.ACTIVE, SecretChatMapper.mapState(active))
        assertEquals(SecretChatState.DISCARDED, SecretChatMapper.mapState(discarded))
        assertEquals(SecretChatState.UNKNOWN, SecretChatMapper.mapState(null))
    }

    @Test
    fun testSecretChatMapperToDomain() {
        val chat = TLRPC.TL_encryptedChat().apply {
            id = 100
            user_id = 555L
            admin_id = 999L
            ttl = 60
            key_fingerprint = 123456789L
            date = 1700000000
        }
        val user = TLRPC.TL_user().apply {
            id = 555L
            first_name = "Alice"
            last_name = "Smith"
        }

        val domain = SecretChatMapper.toDomain(chat, user, currentUserId = 999L)
        assertNotNull(domain)
        assertEquals(100, domain?.chatId)
        assertEquals(DialogObject.makeEncryptedDialogId(100L), domain?.dialogId)
        assertEquals(555L, domain?.userId)
        assertEquals("Alice Smith", domain?.userName)
        assertEquals(SecretChatState.ACTIVE, domain?.state)
        assertEquals(60, domain?.ttlSeconds)
        assertEquals(123456789L, domain?.keyFingerprint)
        assertTrue(domain?.isCreator == true)
        assertEquals(1700000000, domain?.createdAt)
    }

    @Test
    fun testStartSecretChatUseCase() = runTest {
        val repo = FakeSecretChatRepository()
        val useCase = StartSecretChatUseCase(repo)

        val result = useCase(userId = 42L)
        assertTrue(result is Result.Success)
        val chatId = (result as Result.Success).data
        val createdChat = repo.getSecretChat(chatId)
        assertNotNull(createdChat)
        assertEquals(SecretChatState.WAITING, createdChat?.state)
        assertEquals(42L, createdChat?.userId)
    }

    @Test
    fun testAcceptSecretChatUseCase() = runTest {
        val repo = FakeSecretChatRepository()
        repo.putChat(SecretChatModel(chatId = 1, dialogId = 1L, userId = 2L, state = SecretChatState.REQUESTED))
        val useCase = AcceptSecretChatUseCase(repo)

        val result = useCase(chatId = 1)
        assertTrue(result is Result.Success)
        assertEquals(SecretChatState.ACTIVE, repo.getSecretChat(1)?.state)
    }

    @Test
    fun testDeclineSecretChatUseCase() = runTest {
        val repo = FakeSecretChatRepository()
        repo.putChat(SecretChatModel(chatId = 1, dialogId = 1L, userId = 2L, state = SecretChatState.REQUESTED))
        val useCase = DeclineSecretChatUseCase(repo)

        val result = useCase(chatId = 1)
        assertTrue(result is Result.Success)
        assertEquals(SecretChatState.DISCARDED, repo.getSecretChat(1)?.state)
    }

    @Test
    fun testSetSecretChatTtlUseCase() = runTest {
        val repo = FakeSecretChatRepository()
        repo.putChat(SecretChatModel(chatId = 1, dialogId = 1L, userId = 2L, ttlSeconds = 0))
        val useCase = SetSecretChatTtlUseCase(repo)

        val result = useCase(chatId = 1, ttlSeconds = 30)
        assertTrue(result is Result.Success)
        assertEquals(30, repo.getSecretChat(1)?.ttlSeconds)
    }

    @Test
    fun testSendScreenshotNotificationUseCase() = runTest {
        val repo = FakeSecretChatRepository()
        val useCase = SendScreenshotNotificationUseCase(repo)

        val result = useCase(chatId = 1)
        assertTrue(result is Result.Success)
    }

    @Test
    fun testObserveSecretChatAndChatsUseCases() = runTest {
        val repo = FakeSecretChatRepository()
        val chat1 = SecretChatModel(chatId = 1, dialogId = 1L, userId = 2L, state = SecretChatState.ACTIVE)
        val chat2 = SecretChatModel(chatId = 2, dialogId = 2L, userId = 3L, state = SecretChatState.ACTIVE)

        repo.putChat(chat1)
        repo.putChat(chat2)

        val observeOne = ObserveSecretChatUseCase(repo)(1).first()
        assertEquals(1, observeOne?.chatId)

        val observeAll = ObserveSecretChatsUseCase(repo)().first()
        assertEquals(2, observeAll.size)
    }

    @Test
    fun testSecretChatViewModelSuccessFlow() = runTest {
        val repo = FakeSecretChatRepository()
        val initialChat = SecretChatModel(
            chatId = 10,
            dialogId = DialogObject.makeEncryptedDialogId(10L),
            userId = 20L,
            userName = "Bob",
            state = SecretChatState.REQUESTED,
            ttlSeconds = 0
        )
        repo.putChat(initialChat)

        val viewModel = SecretChatViewModel(
            chatId = 10,
            observeSecretChatUseCase = ObserveSecretChatUseCase(repo),
            getSecretChatUseCase = GetSecretChatUseCase(repo),
            acceptSecretChatUseCase = AcceptSecretChatUseCase(repo),
            declineSecretChatUseCase = DeclineSecretChatUseCase(repo),
            setSecretChatTtlUseCase = SetSecretChatTtlUseCase(repo),
            sendScreenshotNotificationUseCase = SendScreenshotNotificationUseCase(repo)
        )

        val events = mutableListOf<SecretChatEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SecretChatUiState.Success)
        assertEquals(SecretChatState.REQUESTED, (viewModel.uiState.value as SecretChatUiState.Success).chat.state)

        // Accept
        viewModel.accept()
        advanceUntilIdle()
        assertTrue(events.contains(SecretChatEvent.ChatAccepted))
        assertEquals(SecretChatState.ACTIVE, (viewModel.uiState.value as SecretChatUiState.Success).chat.state)

        // Set TTL
        viewModel.setTtl(15)
        advanceUntilIdle()
        assertTrue(events.any { it is SecretChatEvent.TtlUpdated && it.ttlSeconds == 15 })
        assertEquals(15, (viewModel.uiState.value as SecretChatUiState.Success).chat.ttlSeconds)

        // Send Screenshot
        viewModel.sendScreenshotNotification()
        advanceUntilIdle()
        assertTrue(events.contains(SecretChatEvent.ScreenshotSent))

        // Decline
        viewModel.decline()
        advanceUntilIdle()
        assertTrue(events.contains(SecretChatEvent.ChatDeclined))
        assertEquals(SecretChatState.DISCARDED, (viewModel.uiState.value as SecretChatUiState.Success).chat.state)

        eventsJob.cancel()
    }

    @Test
    fun testSecretChatViewModelErrorHandling() = runTest {
        val repo = FakeSecretChatRepository().apply { shouldSucceed = false }
        val viewModel = SecretChatViewModel(
            chatId = 999,
            observeSecretChatUseCase = ObserveSecretChatUseCase(repo),
            getSecretChatUseCase = GetSecretChatUseCase(repo),
            acceptSecretChatUseCase = AcceptSecretChatUseCase(repo),
            declineSecretChatUseCase = DeclineSecretChatUseCase(repo),
            setSecretChatTtlUseCase = SetSecretChatTtlUseCase(repo),
            sendScreenshotNotificationUseCase = SendScreenshotNotificationUseCase(repo)
        )

        val events = mutableListOf<SecretChatEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SecretChatUiState.Error)

        viewModel.accept()
        advanceUntilIdle()
        assertTrue(events.any { it is SecretChatEvent.ShowError && it.message == "Failed to accept chat" })

        viewModel.decline()
        advanceUntilIdle()
        assertTrue(events.any { it is SecretChatEvent.ShowError && it.message == "Failed to decline chat" })

        viewModel.setTtl(10)
        advanceUntilIdle()
        assertTrue(events.any { it is SecretChatEvent.ShowError && it.message == "Failed to set TTL" })

        eventsJob.cancel()
    }

    @Test
    fun testAccountFeatureContainerWiring() {
        val container = AccountFeatureContainer.get(0)
        val customRepo = FakeSecretChatRepository()
        container.secretChatRepository = customRepo

        assertEquals(customRepo, container.secretChatRepository)
        assertNotNull(container.observeSecretChatUseCase)
        assertNotNull(container.observeSecretChatsUseCase)
        assertNotNull(container.getSecretChatUseCase)
        assertNotNull(container.startSecretChatUseCase)
        assertNotNull(container.acceptSecretChatUseCase)
        assertNotNull(container.declineSecretChatUseCase)
        assertNotNull(container.setSecretChatTtlUseCase)
        assertNotNull(container.sendScreenshotNotificationUseCase)
        assertNotNull(container.getSecretChatViewModel(123))

        AccountFeatureContainer.reset(0)
    }
}
