package org.telegram.messenger.feature.folders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.folders.data.mapper.FolderMapper
import org.telegram.messenger.feature.folders.domain.model.FolderModel
import org.telegram.messenger.feature.folders.domain.model.SuggestedFolderModel
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository
import org.telegram.messenger.feature.folders.domain.usecase.CreateFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.DeleteFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetSuggestedFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.ObserveFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.ReorderFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.UpdateFolderUseCase
import org.telegram.messenger.feature.folders.presentation.FoldersEvent
import org.telegram.messenger.feature.folders.presentation.FoldersUiState
import org.telegram.messenger.feature.folders.presentation.FoldersViewModel
import org.telegram.messenger.support.LongSparseIntArray
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class FoldersDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeFoldersRepository : FoldersRepository {
        val foldersMap = mutableMapOf<Int, FolderModel>()
        val foldersFlow = MutableSharedFlow<List<FolderModel>>(replay = 1)
        val suggestedList = mutableListOf<SuggestedFolderModel>()
        var shouldSucceed = true

        fun putFolder(folder: FolderModel) {
            foldersMap[folder.id] = folder
            foldersFlow.tryEmit(foldersMap.values.toList())
        }

        fun removeFolder(id: Int) {
            foldersMap.remove(id)
            foldersFlow.tryEmit(foldersMap.values.toList())
        }

        override fun observeFolders(): Flow<List<FolderModel>> = foldersFlow.asSharedFlow()

        override suspend fun getFolders(): List<FolderModel> = foldersMap.values.toList()

        override suspend fun getFolder(id: Int): FolderModel? = foldersMap[id]

        override suspend fun createFolder(
            name: String,
            flags: Int,
            includedPeerIds: List<Long>,
            excludedPeerIds: List<Long>,
            pinnedPeerIds: List<Long>,
            color: Int
        ): Result<FolderModel> {
            return if (shouldSucceed) {
                val newId = (foldersMap.keys.maxOrNull() ?: 1) + 1
                val folder = FolderModel(
                    id = newId,
                    name = name,
                    order = foldersMap.size,
                    flags = flags,
                    color = color,
                    includedPeerIds = includedPeerIds,
                    excludedPeerIds = excludedPeerIds,
                    pinnedPeerIds = pinnedPeerIds
                )
                putFolder(folder)
                Result.success(folder)
            } else {
                Result.failure(AppError.Generic("Failed to create folder"))
            }
        }

        override suspend fun updateFolder(folder: FolderModel): Result<Unit> {
            return if (shouldSucceed) {
                if (foldersMap.containsKey(folder.id)) {
                    putFolder(folder)
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Folder not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to update folder"))
            }
        }

        override suspend fun deleteFolder(id: Int): Result<Unit> {
            return if (shouldSucceed) {
                if (foldersMap.containsKey(id)) {
                    removeFolder(id)
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Folder not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to delete folder"))
            }
        }

        override suspend fun reorderFolders(folderIds: List<Int>): Result<Unit> {
            return if (shouldSucceed) {
                val reordered = mutableMapOf<Int, FolderModel>()
                folderIds.forEachIndexed { index, id ->
                    val f = foldersMap[id]
                    if (f != null) {
                        reordered[id] = f.copy(order = index)
                    }
                }
                foldersMap.clear()
                foldersMap.putAll(reordered)
                foldersFlow.tryEmit(foldersMap.values.toList())
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to reorder folders"))
            }
        }

        override suspend fun getSuggestedFolders(): List<SuggestedFolderModel> = suggestedList
    }

    @Test
    fun testFolderMapperFromDialogFilter() {
        val filter = MessagesController.DialogFilter().apply {
            id = 2
            name = "Work"
            unreadCount = 5
            order = 1
            flags = MessagesController.DIALOG_FILTER_FLAG_CHANNELS
            color = 3
            alwaysShow = ArrayList(listOf(100L, 200L))
            neverShow = ArrayList(listOf(300L))
            val pinned = LongSparseIntArray()
            pinned.put(100L, 0)
            pinnedDialogs = pinned
        }

        val domainModel = FolderMapper.toDomain(filter)

        assertEquals(2, domainModel.id)
        assertEquals("Work", domainModel.name)
        assertEquals(5, domainModel.unreadCount)
        assertEquals(1, domainModel.order)
        assertEquals(MessagesController.DIALOG_FILTER_FLAG_CHANNELS, domainModel.flags)
        assertEquals(3, domainModel.color)
        assertEquals(listOf(100L, 200L), domainModel.includedPeerIds)
        assertEquals(listOf(300L), domainModel.excludedPeerIds)
        assertEquals(listOf(100L), domainModel.pinnedPeerIds)
        assertFalse(domainModel.isDefault)
    }

    @Test
    fun testFolderMapperNullHandling() {
        val nullModel = FolderMapper.toDomain(null as MessagesController.DialogFilter?)
        assertEquals(0, nullModel.id)
        assertEquals("", nullModel.name)

        val nullSuggested = FolderMapper.toDomain(null as TLRPC.TL_dialogFilterSuggested?)
        assertEquals(0, nullSuggested.id)
        assertEquals("", nullSuggested.description)
        assertEquals(0, nullSuggested.folder.id)
    }

    @Test
    fun testFolderMapperFromSuggested() {
        val suggested = TLRPC.TL_dialogFilterSuggested().apply {
            description = "Unread messages"
            filter = TLRPC.TL_dialogFilter().apply {
                id = 5
                title = TLRPC.TL_textWithEntities().apply { text = "Unread" }
                groups = true
                bots = true
                exclude_muted = true
                include_peers = ArrayList(listOf(
                    TLRPC.TL_inputPeerUser().apply { user_id = 777L }
                ))
                exclude_peers = ArrayList(listOf(
                    TLRPC.TL_inputPeerChat().apply { chat_id = 888L }
                ))
            }
        }

        val domainModel = FolderMapper.toDomain(suggested)

        assertEquals(5, domainModel.id)
        assertEquals("Unread messages", domainModel.description)
        assertEquals(5, domainModel.folder.id)
        assertEquals("Unread", domainModel.folder.name)
        assertTrue(domainModel.folder.flags and MessagesController.DIALOG_FILTER_FLAG_GROUPS != 0)
        assertTrue(domainModel.folder.flags and MessagesController.DIALOG_FILTER_FLAG_BOTS != 0)
        assertTrue(domainModel.folder.flags and MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED != 0)
        assertEquals(listOf(777L), domainModel.folder.includedPeerIds)
        assertEquals(listOf(-888L), domainModel.folder.excludedPeerIds)
    }

    @Test
    fun testObserveAndGetFoldersUseCases() = runTest {
        val repo = FakeFoldersRepository()
        val f1 = FolderModel(id = 1, name = "All", isDefault = true)
        val f2 = FolderModel(id = 2, name = "Work")
        repo.putFolder(f1)
        repo.putFolder(f2)

        val observeUseCase = ObserveFoldersUseCase(repo)
        val getFoldersUseCase = GetFoldersUseCase(repo)
        val getFolderUseCase = GetFolderUseCase(repo)

        val list = getFoldersUseCase()
        assertEquals(2, list.size)

        val single = getFolderUseCase(2)
        assertNotNull(single)
        assertEquals("Work", single?.name)

        val notFound = getFolderUseCase(999)
        assertNull(notFound)

        var emitted: List<FolderModel>? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            observeUseCase().collect { emitted = it }
        }

        advanceUntilIdle()
        assertNotNull(emitted)
        assertEquals(2, emitted?.size)

        job.cancel()
    }

    @Test
    fun testCreateUpdateDeleteFolderUseCases() = runTest {
        val repo = FakeFoldersRepository()
        val createUseCase = CreateFolderUseCase(repo)
        val updateUseCase = UpdateFolderUseCase(repo)
        val deleteUseCase = DeleteFolderUseCase(repo)

        val createResult = createUseCase("Personal", MessagesController.DIALOG_FILTER_FLAG_CONTACTS)
        assertTrue(createResult.isSuccess)
        val created = (createResult as Result.Success).data
        assertEquals("Personal", created.name)
        assertEquals(1, repo.getFolders().size)

        val updateResult = updateUseCase(created.copy(name = "Personal & Family"))
        assertTrue(updateResult.isSuccess)
        assertEquals("Personal & Family", repo.getFolder(created.id)?.name)

        val deleteResult = deleteUseCase(created.id)
        assertTrue(deleteResult.isSuccess)
        assertEquals(0, repo.getFolders().size)

        // Error path
        repo.shouldSucceed = false
        val failCreate = createUseCase("Fail")
        assertTrue(failCreate.isFailure)

        val failUpdate = updateUseCase(created)
        assertTrue(failUpdate.isFailure)

        val failDelete = deleteUseCase(created.id)
        assertTrue(failDelete.isFailure)
    }

    @Test
    fun testReorderAndSuggestedFoldersUseCases() = runTest {
        val repo = FakeFoldersRepository()
        val f1 = FolderModel(id = 1, name = "First", order = 0)
        val f2 = FolderModel(id = 2, name = "Second", order = 1)
        repo.putFolder(f1)
        repo.putFolder(f2)

        val reorderUseCase = ReorderFoldersUseCase(repo)
        val reorderResult = reorderUseCase(listOf(2, 1))
        assertTrue(reorderResult.isSuccess)

        val reordered = repo.getFolders()
        assertEquals(2, reordered.find { it.id == 2 }?.id)
        assertEquals(0, reordered.find { it.id == 2 }?.order)
        assertEquals(1, reordered.find { it.id == 1 }?.order)

        val suggestedFolder = SuggestedFolderModel(10, "Bots", FolderModel(10, "Bots"))
        repo.suggestedList.add(suggestedFolder)
        val suggestedUseCase = GetSuggestedFoldersUseCase(repo)
        val suggested = suggestedUseCase()
        assertEquals(1, suggested.size)
        assertEquals("Bots", suggested.first().description)
    }

    @Test
    fun testFoldersViewModelFlowAndEvents() = runTest {
        val repo = FakeFoldersRepository()
        val f1 = FolderModel(id = 1, name = "All", isDefault = true)
        val f2 = FolderModel(id = 2, name = "News")
        repo.putFolder(f1)
        repo.putFolder(f2)
        repo.suggestedList.add(SuggestedFolderModel(99, "Work", FolderModel(99, "Work")))

        val viewModel = FoldersViewModel(
            observeFoldersUseCase = ObserveFoldersUseCase(repo),
            getFoldersUseCase = GetFoldersUseCase(repo),
            getFolderUseCase = GetFolderUseCase(repo),
            createFolderUseCase = CreateFolderUseCase(repo),
            updateFolderUseCase = UpdateFolderUseCase(repo),
            deleteFolderUseCase = DeleteFolderUseCase(repo),
            reorderFoldersUseCase = ReorderFoldersUseCase(repo),
            getSuggestedFoldersUseCase = GetSuggestedFoldersUseCase(repo)
        )

        val events = mutableListOf<FoldersEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()

        // Verify initial state
        assertTrue(viewModel.uiState.value is FoldersUiState.Success)
        val state = viewModel.uiState.value as FoldersUiState.Success
        assertEquals(2, state.folders.size)
        assertEquals(1, state.suggested.size)

        // Create folder
        viewModel.createFolder("NewFolder")
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.FolderCreated })

        // Update folder
        viewModel.updateFolder(f2.copy(name = "News & Media"))
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.FolderUpdated && it.folderId == 2 })

        // Delete folder
        viewModel.deleteFolder(2)
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.FolderDeleted && it.folderId == 2 })

        // Reorder
        viewModel.reorderFolders(listOf(1))
        advanceUntilIdle()
        assertTrue(events.contains(FoldersEvent.FoldersReordered))

        // Refresh
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is FoldersUiState.Success)

        eventsJob.cancel()
    }

    @Test
    fun testFoldersViewModelErrorHandling() = runTest {
        val repo = FakeFoldersRepository().apply { shouldSucceed = false }

        val viewModel = FoldersViewModel(
            observeFoldersUseCase = ObserveFoldersUseCase(repo),
            getFoldersUseCase = GetFoldersUseCase(repo),
            getFolderUseCase = GetFolderUseCase(repo),
            createFolderUseCase = CreateFolderUseCase(repo),
            updateFolderUseCase = UpdateFolderUseCase(repo),
            deleteFolderUseCase = DeleteFolderUseCase(repo),
            reorderFoldersUseCase = ReorderFoldersUseCase(repo),
            getSuggestedFoldersUseCase = GetSuggestedFoldersUseCase(repo)
        )

        val events = mutableListOf<FoldersEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.createFolder("Fail")
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.ShowError && it.message == "Failed to create folder" })

        viewModel.updateFolder(FolderModel(1, "Fail"))
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.ShowError && it.message == "Failed to update folder" })

        viewModel.deleteFolder(1)
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.ShowError && it.message == "Failed to delete folder" })

        viewModel.reorderFolders(listOf(1))
        advanceUntilIdle()
        assertTrue(events.any { it is FoldersEvent.ShowError && it.message == "Failed to reorder folders" })

        eventsJob.cancel()
    }

    @Test
    fun testAccountFeatureContainerWiring() {
        val container = AccountFeatureContainer.get(0)
        val customRepo = FakeFoldersRepository()
        container.foldersRepository = customRepo

        assertEquals(customRepo, container.foldersRepository)
        assertNotNull(container.observeFoldersUseCase)
        assertNotNull(container.getFoldersUseCase)
        assertNotNull(container.getFolderUseCase)
        assertNotNull(container.createFolderUseCase)
        assertNotNull(container.updateFolderUseCase)
        assertNotNull(container.deleteFolderUseCase)
        assertNotNull(container.reorderFoldersUseCase)
        assertNotNull(container.getSuggestedFoldersUseCase)
        assertNotNull(container.foldersViewModel)

        AccountFeatureContainer.reset(0)
    }
}
