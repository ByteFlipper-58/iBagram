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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.model.SavedTagModel
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.usecase.DeleteSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedTagsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.SearchSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesEvent
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesUiState
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesViewModel
import org.telegram.tgnet.TLRPC

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
        val tagsFlow = MutableSharedFlow<List<SavedTagModel>>(replay = 1)
        val currentDialogs = mutableListOf<SavedDialogModel>()
        var pinSuccess = true
        var deleteSuccess = true

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
            return if (deleteSuccess) {
                currentDialogs.removeAll { it.dialogId == dialogId }
                dialogsFlow.emit(currentDialogs.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Delete failed"))
            }
        }

        override fun observeSavedTags(): Flow<List<SavedTagModel>> = tagsFlow.asSharedFlow()

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
    fun testDeleteSavedDialogUseCase() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummy = SavedDialogModel(
            dialogId = 555L,
            title = "To Delete",
            isPinned = false,
            unreadCount = 0,
            messagesCount = 1,
            lastMessageDate = 1700000000,
            topMessageId = 1
        )
        fakeRepo.currentDialogs.add(dummy)

        val deleteUseCase = DeleteSavedDialogUseCase(fakeRepo)
        val result = deleteUseCase(555L)
        assertTrue(result.isSuccess)
        assertTrue(fakeRepo.currentDialogs.isEmpty())

        fakeRepo.deleteSuccess = false
        val failResult = deleteUseCase(555L)
        assertTrue(failResult.isFailure)
    }

    @Test
    fun testSearchSavedDialogsUseCase() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        fakeRepo.currentDialogs.add(
            SavedDialogModel(1L, "Telegram News", false, 0, 5, 1700000000, 10)
        )
        fakeRepo.currentDialogs.add(
            SavedDialogModel(2L, "Personal Notes", true, 0, 20, 1700000000, 20)
        )

        val searchUseCase = SearchSavedDialogsUseCase(fakeRepo)
        val newsResults = searchUseCase("News")
        assertEquals(1, newsResults.size)
        assertEquals(1L, newsResults[0].dialogId)

        val emptyResults = searchUseCase("Nonexistent")
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun testGetSavedTagsUseCase() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val tag = SavedTagModel(reaction = "👍", title = "Thumbs Up", count = 5)
        val useCase = GetSavedTagsUseCase(fakeRepo)

        val collected = mutableListOf<List<SavedTagModel>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase.observe().toList(collected)
        }

        fakeRepo.tagsFlow.emit(listOf(tag))
        assertEquals(1, collected.size)
        assertEquals(1, collected[0].size)
        assertEquals("👍", collected[0][0].reaction)

        job.cancel()
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
    fun testViewModelSearchAndFiltering() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val item1 = SavedDialogModel(1L, "Alpha", false, 0, 1, 1700, 1)
        val item2 = SavedDialogModel(2L, "Beta", false, 0, 1, 1700, 2)
        fakeRepo.currentDialogs.addAll(listOf(item1, item2))
        fakeRepo.dialogsFlow.emit(listOf(item1, item2))

        val viewModel = SavedMessagesViewModel(
            account = 0,
            getSavedDialogsUseCase = GetSavedDialogsUseCase(fakeRepo),
            togglePinSavedDialogUseCase = TogglePinSavedDialogUseCase(fakeRepo),
            searchSavedDialogsUseCase = SearchSavedDialogsUseCase(fakeRepo)
        )
        advanceUntilIdle()

        // Perform search
        viewModel.onSearch("alp")
        val searchState = viewModel.uiState.value as SavedMessagesUiState.Content
        assertEquals("alp", searchState.searchQuery)
        assertEquals(1, searchState.searchResults?.size)
        assertEquals("Alpha", searchState.displayedDialogs[0].title)

        // Clear search
        viewModel.onSearch("")
        val clearedState = viewModel.uiState.value as SavedMessagesUiState.Content
        assertNull(clearedState.searchResults)
        assertEquals(2, clearedState.displayedDialogs.size)
    }

    @Test
    fun testViewModelDeleteDialogAndFailureEvent() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummy = SavedDialogModel(10L, "ToDelete", false, 0, 1, 1700, 1)
        fakeRepo.currentDialogs.add(dummy)
        fakeRepo.dialogsFlow.emit(listOf(dummy))

        val viewModel = SavedMessagesViewModel(
            account = 0,
            getSavedDialogsUseCase = GetSavedDialogsUseCase(fakeRepo),
            togglePinSavedDialogUseCase = TogglePinSavedDialogUseCase(fakeRepo),
            deleteSavedDialogUseCase = DeleteSavedDialogUseCase(fakeRepo)
        )
        advanceUntilIdle()

        val events = mutableListOf<SavedMessagesEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        // Test delete failure
        fakeRepo.deleteSuccess = false
        viewModel.onDeleteDialog(dummy)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is SavedMessagesEvent.ShowError)
        assertEquals("Delete failed", (events[0] as SavedMessagesEvent.ShowError).message)
    }

    @Test
    fun testViewModelTogglePinFailureEvent() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val dummy = SavedDialogModel(20L, "PinTest", false, 0, 1, 1700, 1)
        fakeRepo.currentDialogs.add(dummy)
        fakeRepo.dialogsFlow.emit(listOf(dummy))
        fakeRepo.pinSuccess = false

        val viewModel = SavedMessagesViewModel(
            account = 0,
            getSavedDialogsUseCase = GetSavedDialogsUseCase(fakeRepo),
            togglePinSavedDialogUseCase = TogglePinSavedDialogUseCase(fakeRepo)
        )
        advanceUntilIdle()

        val events = mutableListOf<SavedMessagesEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onTogglePin(dummy)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is SavedMessagesEvent.ShowError)
        assertEquals("Pin limit reached", (events[0] as SavedMessagesEvent.ShowError).message)
    }

    @Test
    fun testViewModelTagsObservationAndStatePreservation() = runTest(testDispatcher) {
        val fakeRepo = FakeSavedMessagesRepository()
        val item1 = SavedDialogModel(1L, "First", false, 0, 1, 1700, 1)
        val tag1 = SavedTagModel(reaction = "⭐", title = "Star", count = 3)

        fakeRepo.currentDialogs.add(item1)
        fakeRepo.dialogsFlow.emit(listOf(item1))

        val viewModel = SavedMessagesViewModel(
            account = 0,
            getSavedDialogsUseCase = GetSavedDialogsUseCase(fakeRepo),
            togglePinSavedDialogUseCase = TogglePinSavedDialogUseCase(fakeRepo),
            getSavedTagsUseCase = GetSavedTagsUseCase(fakeRepo)
        )
        advanceUntilIdle()

        fakeRepo.tagsFlow.emit(listOf(tag1))
        advanceUntilIdle()

        val stateWithTags = viewModel.uiState.value as SavedMessagesUiState.Content
        assertEquals(1, stateWithTags.tags.size)
        assertEquals("⭐", stateWithTags.tags[0].reaction)

        // Now emit updated dialogs — verify tags are NOT wiped out!
        val item2 = SavedDialogModel(2L, "Second", false, 0, 2, 1800, 2)
        fakeRepo.currentDialogs.add(item2)
        fakeRepo.dialogsFlow.emit(listOf(item1, item2))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value as SavedMessagesUiState.Content
        assertEquals(2, updatedState.dialogs.size)
        assertEquals(1, updatedState.tags.size) // Preserved!
        assertEquals("⭐", updatedState.tags[0].reaction)
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

    @Test
    fun testSavedMessagesMapperForTags() {
        val emojiTag = TLRPC.TL_savedReactionTag()
        emojiTag.reaction = TLRPC.TL_reactionEmoji().apply { emoticon = "🔥" }
        emojiTag.title = "Fire Tag"
        emojiTag.count = 15

        val domainEmojiTag = org.telegram.messenger.feature.savedmessages.data.mapper.SavedMessagesMapper.mapTagToDomain(emojiTag)
        assertEquals("🔥", domainEmojiTag.reaction)
        assertEquals("Fire Tag", domainEmojiTag.title)
        assertEquals(15, domainEmojiTag.count)

        val customEmojiTag = TLRPC.TL_savedReactionTag()
        customEmojiTag.reaction = TLRPC.TL_reactionCustomEmoji().apply { document_id = 987654321L }
        customEmojiTag.title = "Custom Tag"
        customEmojiTag.count = 7

        val domainCustomTag = org.telegram.messenger.feature.savedmessages.data.mapper.SavedMessagesMapper.mapTagToDomain(customEmojiTag)
        assertEquals("987654321", domainCustomTag.reaction)
        assertEquals("Custom Tag", domainCustomTag.title)
        assertEquals(7, domainCustomTag.count)
    }
}
