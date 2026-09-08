package org.telegram.messenger.feature.dialogs

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
import org.telegram.messenger.feature.dialogs.data.mapper.DialogMapper
import org.telegram.messenger.feature.dialogs.domain.model.DialogModel
import org.telegram.messenger.feature.dialogs.domain.repository.DialogsRepository
import org.telegram.messenger.feature.dialogs.domain.usecase.DeleteDialogUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.GetDialogsUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.LoadMoreDialogsUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.MarkDialogAsReadUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.PinDialogUseCase
import org.telegram.messenger.feature.dialogs.presentation.DialogsEvent
import org.telegram.messenger.feature.dialogs.presentation.DialogsUiState
import org.telegram.messenger.feature.dialogs.presentation.DialogsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class DialogsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDialogsRepository : DialogsRepository {
        val folderFlows = mutableMapOf<Int, MutableSharedFlow<List<DialogModel>>>()
        val currentDialogs = mutableMapOf<Int, MutableList<DialogModel>>()
        var pinSuccess = true
        var deleteSuccess = true
        var loadMoreSuccess = true
        var markAsReadSuccess = true

        private fun getOrCreateFlow(folderId: Int): MutableSharedFlow<List<DialogModel>> {
            return folderFlows.computeIfAbsent(folderId) { MutableSharedFlow(replay = 1) }
        }

        private fun getOrCreateList(folderId: Int): MutableList<DialogModel> {
            return currentDialogs.computeIfAbsent(folderId) { mutableListOf() }
        }

        override fun getDialogs(folderId: Int): Flow<List<DialogModel>> {
            return getOrCreateFlow(folderId).asSharedFlow()
        }

        override suspend fun loadMoreDialogs(folderId: Int, count: Int): Result<Unit> {
            return if (loadMoreSuccess) {
                val list = getOrCreateList(folderId)
                val newDialog = DialogModel(id = (list.size + 100).toLong(), folderId = folderId)
                list.add(newDialog)
                getOrCreateFlow(folderId).emit(list.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Load more failed"))
            }
        }

        override suspend fun pinDialog(dialogId: Long, pin: Boolean): Result<Unit> {
            return if (pinSuccess) {
                currentDialogs.values.forEach { list ->
                    val idx = list.indexOfFirst { it.id == dialogId }
                    if (idx != -1) {
                        list[idx] = list[idx].copy(isPinned = pin)
                        getOrCreateFlow(list[idx].folderId).emit(list.toList())
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Pin limit reached"))
            }
        }

        override suspend fun deleteDialog(dialogId: Long, revoke: Boolean): Result<Unit> {
            return if (deleteSuccess) {
                currentDialogs.values.forEach { list ->
                    val removed = list.removeAll { it.id == dialogId }
                    if (removed) {
                        getOrCreateFlow(0).emit(list.toList())
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Delete failed"))
            }
        }

        override suspend fun markAsRead(dialogId: Long): Result<Unit> {
            return if (markAsReadSuccess) {
                currentDialogs.values.forEach { list ->
                    val idx = list.indexOfFirst { it.id == dialogId }
                    if (idx != -1) {
                        list[idx] = list[idx].copy(unreadCount = 0)
                        getOrCreateFlow(list[idx].folderId).emit(list.toList())
                    }
                }
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Mark as read failed"))
            }
        }
    }

    @Test
    fun testDialogMapper() {
        val legacy = TLRPC.TL_dialog().apply {
            id = 12345L
            unread_count = 3
            unread_mentions_count = 1
            unread_reactions_count = 2
            last_message_date = 1700000000
            top_message = 456
            pinned = true
            pinnedNum = 1
            folder_id = 0
            draft = TLRPC.TL_draftMessage().apply {
                message = "Hello draft"
            }
        }

        val domain = DialogMapper.mapToDomain(legacy, isMuted = true, isForum = false)

        assertEquals(12345L, domain.id)
        assertEquals(3, domain.unreadCount)
        assertEquals(1, domain.unreadMentionsCount)
        assertEquals(2, domain.unreadReactionsCount)
        assertEquals(1700000000, domain.lastMessageDate)
        assertEquals(456, domain.lastMessageId)
        assertTrue(domain.isPinned)
        assertEquals(1, domain.pinnedNum)
        assertTrue(domain.isMuted)
        assertFalse(domain.isForum)
        assertEquals("Hello draft", domain.draftText)
    }

    @Test
    fun testDialogMapper_noDraft() {
        val legacy = TLRPC.TL_dialog().apply {
            id = 999L
            draft = null
        }

        val domain = DialogMapper.mapToDomain(legacy)

        assertEquals(999L, domain.id)
        assertNull(domain.draftText)
        assertFalse(domain.isMuted)
        assertFalse(domain.isForum)
    }

    @Test
    fun testGetDialogsUseCase() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val useCase = GetDialogsUseCase(repo)

        val testItem = DialogModel(id = 101L, unreadCount = 2)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(testItem)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(testItem))

        val result = useCase(0).first()
        assertEquals(1, result.size)
        assertEquals(101L, result[0].id)
    }

    @Test
    fun testPinDialogUseCase_success() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val item = DialogModel(id = 202L, isPinned = false)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(item))

        val useCase = PinDialogUseCase(repo)
        val result = useCase(202L, true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun testPinDialogUseCase_failure() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository().apply { pinSuccess = false }
        val useCase = PinDialogUseCase(repo)
        val result = useCase(202L, true)

        assertTrue(result.isFailure)
        assertEquals("Pin limit reached", (result as Result.Failure).error.message)
    }

    @Test
    fun testDeleteDialogUseCase() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val item = DialogModel(id = 303L)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)

        val useCase = DeleteDialogUseCase(repo)
        val result = useCase(303L, revoke = true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun testMarkDialogAsReadUseCase() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val item = DialogModel(id = 404L, unreadCount = 5)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)

        val useCase = MarkDialogAsReadUseCase(repo)
        val result = useCase(404L)

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDialogsViewModel_initialStateAndObserve() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val item = DialogModel(id = 505L, unreadCount = 1)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(item))

        val viewModel = DialogsViewModel(
            account = 0,
            getDialogsUseCase = GetDialogsUseCase(repo),
            loadMoreDialogsUseCase = LoadMoreDialogsUseCase(repo),
            pinDialogUseCase = PinDialogUseCase(repo),
            deleteDialogUseCase = DeleteDialogUseCase(repo),
            markDialogAsReadUseCase = MarkDialogAsReadUseCase(repo)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DialogsUiState.Success)
        val success = state as DialogsUiState.Success
        assertEquals(1, success.dialogs.size)
        assertEquals(505L, success.dialogs[0].id)
        assertEquals(0, success.currentFolderId)
    }

    @Test
    fun testDialogsViewModel_switchFolder() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val mainItem = DialogModel(id = 1L, folderId = 0)
        val archiveItem = DialogModel(id = 2L, folderId = 1)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(mainItem)
        repo.currentDialogs.getOrPut(1) { mutableListOf() }.add(archiveItem)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(mainItem))
        repo.folderFlows.getOrPut(1) { MutableSharedFlow(replay = 1) }.emit(listOf(archiveItem))

        val viewModel = DialogsViewModel(
            account = 0,
            getDialogsUseCase = GetDialogsUseCase(repo),
            loadMoreDialogsUseCase = LoadMoreDialogsUseCase(repo),
            pinDialogUseCase = PinDialogUseCase(repo),
            deleteDialogUseCase = DeleteDialogUseCase(repo),
            markDialogAsReadUseCase = MarkDialogAsReadUseCase(repo)
        )

        advanceUntilIdle()
        assertEquals(1L, (viewModel.uiState.value as DialogsUiState.Success).dialogs[0].id)

        viewModel.switchFolder(1)
        advanceUntilIdle()

        val archiveState = viewModel.uiState.value as DialogsUiState.Success
        assertEquals(1, archiveState.currentFolderId)
        assertEquals(2L, archiveState.dialogs[0].id)
    }

    @Test
    fun testDialogsViewModel_onLoadMore() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository()
        val item = DialogModel(id = 10L, folderId = 0)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(item))

        val viewModel = DialogsViewModel(
            account = 0,
            getDialogsUseCase = GetDialogsUseCase(repo),
            loadMoreDialogsUseCase = LoadMoreDialogsUseCase(repo),
            pinDialogUseCase = PinDialogUseCase(repo),
            deleteDialogUseCase = DeleteDialogUseCase(repo),
            markDialogAsReadUseCase = MarkDialogAsReadUseCase(repo)
        )

        advanceUntilIdle()
        viewModel.onLoadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as DialogsUiState.Success
        assertEquals(2, state.dialogs.size)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun testDialogsViewModel_errorDispatchedOnFailure() = runTest(testDispatcher) {
        val repo = FakeDialogsRepository().apply { pinSuccess = false }
        val item = DialogModel(id = 15L)
        repo.currentDialogs.getOrPut(0) { mutableListOf() }.add(item)
        repo.folderFlows.getOrPut(0) { MutableSharedFlow(replay = 1) }.emit(listOf(item))

        val viewModel = DialogsViewModel(
            account = 0,
            getDialogsUseCase = GetDialogsUseCase(repo),
            loadMoreDialogsUseCase = LoadMoreDialogsUseCase(repo),
            pinDialogUseCase = PinDialogUseCase(repo),
            deleteDialogUseCase = DeleteDialogUseCase(repo),
            markDialogAsReadUseCase = MarkDialogAsReadUseCase(repo)
        )

        advanceUntilIdle()

        val events = mutableListOf<DialogsEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onTogglePin(15L, true)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is DialogsEvent.ShowError)
        assertEquals("Pin limit reached", (events[0] as DialogsEvent.ShowError).message)

        job.cancel()
    }
}
