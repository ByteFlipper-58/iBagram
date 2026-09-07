package org.telegram.messenger.feature.savedmessages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.model.SavedTagModel
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesEvent
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesUiState
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SavedMessagesDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSavedMessagesRepository : SavedMessagesRepository {
        val dialogsFlow = MutableSharedFlow<List<SavedDialogModel>>(replay = 1)
        val currentDialogs = mutableListOf<SavedDialogModel>()
        var pinSuccess = true

        override fun observeSavedDialogs(): Flow<List<SavedDialogModel>> = dialogsFlow.asSharedFlow()

        override suspend fun getSavedDialogs(): Result<List<SavedDialogModel>> =
            Result.success(currentDialogs.toList())

        override suspend fun loadDialogs(onlyCache: Boolean): Result<Unit> {
            dialogsFlow.emit(currentDialogs.toList())
            return Result.success(Unit)
        }

        override suspend fun togglePin(dialogId: Long, pinned: Boolean): Result<Unit> {
            return if (pinSuccess) {
                val idx = currentDialogs.indexOfFirst { it.dialogId == dialogId }
                if (idx != -1) {
                    currentDialogs[idx] = currentDialogs[idx].copy(isPinned = pinned)
                    dialogsFlow.emit(currentDialogs.toList())
                }
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Pin limit reached"))
            }
        }

        override suspend fun deleteDialog(dialogId: Long): Result<Unit> {
            currentDialogs.removeAll { it.dialogId == dialogId }
            dialogsFlow.emit(currentDialogs.toList())
            return Result.success(Unit)
        }

        override fun observeSavedTags(): Flow<List<SavedTagModel>> =
            MutableSharedFlow<List<SavedTagModel>>()

        override fun searchDialogs(query: String): List<SavedDialogModel> =
            currentDialogs.filter { it.title.contains(query, ignoreCase = true) }
    }

    @Test
    fun testGetSavedDialogsUseCase() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummyDialog = SavedDialogModel(
            dialogId = 12345L,
            title = "Saved Chat",
            isPinned = false,
            unreadCount = 0,
            messagesCount = 10,
            lastMessageDate = 1700000000,
            topMessageId = 100
        )
        fakeRepo.currentDialogs.add(dummyDialog)

        val useCase = GetSavedDialogsUseCase(fakeRepo)
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Saved Chat", result.getOrNull()?.first()?.title)
    }

    @Test
    fun testTogglePinUseCase() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummyDialog = SavedDialogModel(
            dialogId = 12345L,
            title = "Saved Chat",
            isPinned = false,
            unreadCount = 0,
            messagesCount = 10,
            lastMessageDate = 1700000000,
            topMessageId = 100
        )
        fakeRepo.currentDialogs.add(dummyDialog)

        val togglePinUseCase = TogglePinSavedDialogUseCase(fakeRepo)

        // Test successful pin
        val success = togglePinUseCase(12345L, true)
        assertTrue(success.isSuccess)
        assertTrue(fakeRepo.currentDialogs.first().isPinned)

        // Test pin failure
        fakeRepo.pinSuccess = false
        val failure = togglePinUseCase(12345L, false)
        assertTrue(failure.isFailure)
    }

    @Test
    fun testViewModelStateAndClickEvent() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummy = SavedDialogModel(
            dialogId = 999L,
            title = "Notes",
            isPinned = true,
            unreadCount = 2,
            messagesCount = 50,
            lastMessageDate = 1700000000,
            topMessageId = 500
        )
        fakeRepo.currentDialogs.add(dummy)
        fakeRepo.dialogsFlow.emit(listOf(dummy))

        val getUseCase = GetSavedDialogsUseCase(fakeRepo)
        val pinUseCase = TogglePinSavedDialogUseCase(fakeRepo)
        val viewModel = SavedMessagesViewModel(
            account = 0,
            getSavedDialogsUseCase = getUseCase,
            togglePinSavedDialogUseCase = pinUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SavedMessagesUiState.Content)
        assertEquals(1, (state as SavedMessagesUiState.Content).dialogs.size)
        assertEquals("Notes", state.dialogs[0].title)

        val collectedEvents = mutableListOf<SavedMessagesEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(collectedEvents)
        }

        // Test onDialogClicked event emission
        viewModel.onDialogClicked(dummy)
        advanceUntilIdle()

        assertEquals(1, collectedEvents.size)
        val event = collectedEvents.first()
        assertTrue(event is SavedMessagesEvent.NavigateToChat)
        assertEquals(999L, (event as SavedMessagesEvent.NavigateToChat).dialogId)
    }

    @Test
    fun testSavedMessagesMapper() {
        val legacy = org.telegram.messenger.SavedMessagesController.SavedDialog()
        legacy.dialogId = 777L
        legacy.pinned = true
        legacy.unreadCount = 5L
        legacy.messagesCount = 42
        legacy.top_message_id = 101

        val domainModel = org.telegram.messenger.feature.savedmessages.data.mapper.SavedMessagesMapper.mapToDomain(
            legacyDialog = legacy,
            resolvedTitle = "Test Channel",
            snippet = "Hello World"
        )

        assertEquals(777L, domainModel.dialogId)
        assertEquals("Test Channel", domainModel.title)
        assertTrue(domainModel.isPinned)
        assertEquals(5L, domainModel.unreadCount)
        assertEquals(42, domainModel.messagesCount)
        assertEquals(101, domainModel.topMessageId)
        assertEquals("Hello World", domainModel.topMessageSnippet)
    }
}
