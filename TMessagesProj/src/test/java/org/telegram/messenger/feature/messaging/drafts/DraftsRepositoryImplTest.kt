package org.telegram.messenger.feature.messaging.drafts

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.drafts.data.datasource.DraftsLocalDataSource
import org.telegram.messenger.feature.messaging.drafts.data.datasource.DraftsRemoteDataSource
import org.telegram.messenger.feature.messaging.drafts.data.repository.DraftsRepositoryImpl
import org.telegram.messenger.feature.messaging.drafts.domain.model.StoryDraftModel

class DraftsRepositoryImplTest {

    private lateinit var localDataSource: DraftsLocalDataSource
    private lateinit var remoteDataSource: DraftsRemoteDataSource
    private lateinit var repository: DraftsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = DraftsLocalDataSource(0)
        remoteDataSource = DraftsRemoteDataSource(0)
        repository = DraftsRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_isEmpty() {
        val state = repository.getDraftsState()
        assertTrue(state.drafts.isEmpty())
    }

    @Test
    fun saveDraft_updatesStateFlow() = runBlocking {
        val draft = StoryDraftModel(id = 1L, caption = "My test story")
        repository.saveDraft(draft)

        val state = repository.observeDraftsState().first()
        assertEquals(1, state.drafts.size)
        assertEquals("My test story", state.drafts[0].caption)
    }

    @Test
    fun deleteDraft_removesFromState() = runBlocking {
        val draft1 = StoryDraftModel(id = 1L)
        val draft2 = StoryDraftModel(id = 2L)
        repository.saveDraft(draft1)
        repository.saveDraft(draft2)
        assertEquals(2, repository.getDraftsState().drafts.size)

        repository.deleteDraft(1L)
        val state = repository.getDraftsState()
        assertEquals(1, state.drafts.size)
        assertEquals(2L, state.drafts[0].id)
    }

    @Test
    fun deleteDrafts_removesMultiple() = runBlocking {
        repository.saveDraft(StoryDraftModel(id = 10L))
        repository.saveDraft(StoryDraftModel(id = 20L))
        repository.saveDraft(StoryDraftModel(id = 30L))

        repository.deleteDrafts(listOf(10L, 30L))
        val state = repository.getDraftsState()
        assertEquals(1, state.drafts.size)
        assertEquals(20L, state.drafts[0].id)
    }

    @Test
    fun editDraft_retrievalAndDelete() = runBlocking {
        val editDraft = StoryDraftModel(id = 5L, editStoryPeerId = 123L, editStoryId = 456)
        repository.saveDraft(editDraft)

        val retrieved = repository.getDraftForEdit(123L, 456)
        assertNotNull(retrieved)
        assertEquals(5L, retrieved?.id)

        repository.deleteForEdit(123L, 456)
        assertNull(repository.getDraftForEdit(123L, 456))
    }

    @Test
    fun cleanupExpiredDrafts_removesExpiredEntries() = runBlocking {
        val now = 1000000L
        val oldDraft = StoryDraftModel(id = 1L, date = now - 100000L)
        val freshDraft = StoryDraftModel(id = 2L, date = now - 1000L)

        repository.saveDraft(oldDraft)
        repository.saveDraft(freshDraft)

        val cleaned = repository.cleanupExpiredDrafts(now, expirationPeriodMs = 50000L)
        assertEquals(listOf(1L), cleaned)
        assertEquals(1, repository.getDraftsState().drafts.size)
        assertEquals(2L, repository.getDraftsState().drafts[0].id)
    }
}
