package org.telegram.messenger.feature.messaging.drafts

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
import org.telegram.messenger.feature.messaging.drafts.data.repository.LegacyDraftsRepository
import org.telegram.messenger.feature.messaging.drafts.domain.model.DraftType
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.CleanupExpiredDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.LoadDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.ObserveDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.SaveDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.presentation.DraftsEvent
import org.telegram.messenger.feature.messaging.drafts.presentation.DraftsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class DraftsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyDraftsRepository
    private lateinit var observeDraftsStateUseCase: ObserveDraftsStateUseCase
    private lateinit var getDraftsStateUseCase: GetDraftsStateUseCase
    private lateinit var loadDraftsUseCase: LoadDraftsUseCase
    private lateinit var saveDraftUseCase: SaveDraftUseCase
    private lateinit var deleteDraftUseCase: DeleteDraftUseCase
    private lateinit var deleteForEditUseCase: DeleteForEditUseCase
    private lateinit var getDraftForEditUseCase: GetDraftForEditUseCase
    private lateinit var cleanupExpiredDraftsUseCase: CleanupExpiredDraftsUseCase
    private lateinit var viewModel: DraftsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyDraftsRepository(account = 0)
        observeDraftsStateUseCase = ObserveDraftsStateUseCase(repository)
        getDraftsStateUseCase = GetDraftsStateUseCase(repository)
        loadDraftsUseCase = LoadDraftsUseCase(repository)
        saveDraftUseCase = SaveDraftUseCase(repository)
        deleteDraftUseCase = DeleteDraftUseCase(repository)
        deleteForEditUseCase = DeleteForEditUseCase(repository)
        getDraftForEditUseCase = GetDraftForEditUseCase(repository)
        cleanupExpiredDraftsUseCase = CleanupExpiredDraftsUseCase(repository)

        viewModel = DraftsViewModel(
            observeDraftsStateUseCase = observeDraftsStateUseCase,
            getDraftsStateUseCase = getDraftsStateUseCase,
            loadDraftsUseCase = loadDraftsUseCase,
            saveDraftUseCase = saveDraftUseCase,
            deleteDraftUseCase = deleteDraftUseCase,
            deleteForEditUseCase = deleteForEditUseCase,
            getDraftForEditUseCase = getDraftForEditUseCase,
            cleanupExpiredDraftsUseCase = cleanupExpiredDraftsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDraftModelPropertiesAndExpiration() {
        val now = 1_000_000_000L
        val sevenDaysMs = 7L * 24 * 3600 * 1000L

        // Fresh draft
        val freshDraft = StoryDraftModel(
            id = 1L,
            date = now - 1000L,
            type = DraftType.NEW
        )
        assertFalse(freshDraft.isEdit)
        assertFalse(freshDraft.isFailed)
        assertFalse(freshDraft.isExpired(now, sevenDaysMs))

        // Expired draft (> 7 days)
        val expiredDraft = StoryDraftModel(
            id = 2L,
            date = now - sevenDaysMs - 1000L,
            type = DraftType.NEW
        )
        assertTrue(expiredDraft.isExpired(now, sevenDaysMs))

        // Edit draft not expired by editExpireDate
        val editDraftFresh = StoryDraftModel(
            id = 3L,
            date = now - 1000L,
            type = DraftType.EDIT,
            editStoryId = 42,
            editStoryPeerId = 12345L,
            editExpireDate = now + 50_000L
        )
        assertTrue(editDraftFresh.isEdit)
        assertFalse(editDraftFresh.isExpired(now, sevenDaysMs))

        // Edit draft expired by editExpireDate
        val editDraftExpired = StoryDraftModel(
            id = 4L,
            date = now - 1000L,
            type = DraftType.EDIT,
            editStoryId = 42,
            editStoryPeerId = 12345L,
            editExpireDate = now - 50L
        )
        assertTrue(editDraftExpired.isExpired(now, sevenDaysMs))
    }

    @Test
    fun testSaveAndRetrieveDrafts() = runTest(testDispatcher) {
        val draft1 = StoryDraftModel(id = 101L, date = 1000L, type = DraftType.NEW, caption = "First")
        val draft2 = StoryDraftModel(id = 102L, date = 2000L, type = DraftType.FAILED, caption = "Second")
        val draft3 = StoryDraftModel(
            id = 103L,
            date = 3000L,
            type = DraftType.EDIT,
            editStoryId = 77,
            editStoryPeerId = 999L
        )

        repository.saveDraft(draft1)
        repository.saveDraft(draft2)
        repository.saveDraft(draft3)

        val state = repository.getDraftsState()
        assertEquals(3, state.totalCount)
        assertEquals(1, state.newDraftsCount)
        assertEquals(1, state.failedDraftsCount)
        assertEquals(1, state.editDraftsCount)
        // Check sorted by date descending: 103 (3000L), 102 (2000L), 101 (1000L)
        assertEquals(103L, state.drafts[0].id)
        assertEquals(102L, state.drafts[1].id)
        assertEquals(101L, state.drafts[2].id)

        // Get for edit
        val editFound = repository.getDraftForEdit(999L, 77)
        assertNotNull(editFound)
        assertEquals(103L, editFound?.id)

        // Delete for edit
        repository.deleteForEdit(999L, 77)
        assertEquals(2, repository.getDraftsState().totalCount)
        assertNull(repository.getDraftForEdit(999L, 77))

        // Delete single
        repository.deleteDraft(102L)
        assertEquals(1, repository.getDraftsState().totalCount)
        assertEquals(101L, repository.getDraftsState().drafts[0].id)
    }

    @Test
    fun testCleanupExpiredDrafts() = runTest(testDispatcher) {
        val now = 1_000_000_000L
        val sevenDaysMs = 7L * 24 * 3600 * 1000L

        val fresh = StoryDraftModel(id = 1L, date = now - 1000L)
        val expired1 = StoryDraftModel(id = 2L, date = now - sevenDaysMs - 5000L)
        val expired2 = StoryDraftModel(
            id = 3L,
            date = now - 1000L,
            type = DraftType.EDIT,
            editStoryId = 1,
            editExpireDate = now - 100L
        )

        repository.saveDraft(fresh)
        repository.saveDraft(expired1)
        repository.saveDraft(expired2)
        assertEquals(3, repository.getDraftsState().totalCount)

        val removed = repository.cleanupExpiredDrafts(now, sevenDaysMs)
        assertEquals(2, removed.size)
        assertTrue(removed.contains(2L))
        assertTrue(removed.contains(3L))

        val remaining = repository.getDraftsState()
        assertEquals(1, remaining.totalCount)
        assertEquals(1L, remaining.drafts[0].id)
    }

    @Test
    fun testDraftsViewModelMviFlow() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val draft1 = StoryDraftModel(id = 10L, type = DraftType.NEW, caption = "Fresh story")
        val draft2 = StoryDraftModel(id = 20L, type = DraftType.FAILED, caption = "Failed story")

        viewModel.onEvent(DraftsEvent.Save(draft1))
        viewModel.onEvent(DraftsEvent.Save(draft2))
        testDispatcher.scheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertEquals(2, uiState.totalCount)
        assertEquals(2, uiState.displayedDrafts.size)

        // Filter by NEW
        viewModel.onEvent(DraftsEvent.FilterByType(DraftType.NEW))
        testDispatcher.scheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(1, uiState.displayedDrafts.size)
        assertEquals(10L, uiState.displayedDrafts[0].id)

        // Select draft
        viewModel.onEvent(DraftsEvent.SelectDraft(draft1))
        testDispatcher.scheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(10L, uiState.selectedDraft?.id)

        // Reset filter
        viewModel.onEvent(DraftsEvent.FilterByType(null))
        testDispatcher.scheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(2, uiState.displayedDrafts.size)

        // Delete draft
        viewModel.onEvent(DraftsEvent.Delete(10L))
        testDispatcher.scheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(1, uiState.totalCount)
        assertEquals(20L, uiState.drafts[0].id)
    }
}
