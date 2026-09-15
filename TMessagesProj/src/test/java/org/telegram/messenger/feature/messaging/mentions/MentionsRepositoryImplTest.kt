package org.telegram.messenger.feature.messaging.mentions

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.mentions.data.datasource.MentionsLocalDataSource
import org.telegram.messenger.feature.messaging.mentions.data.datasource.MentionsRemoteDataSource
import org.telegram.messenger.feature.messaging.mentions.data.repository.MentionsRepositoryImpl
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionCandidate
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionQuery
import org.telegram.messenger.feature.messaging.mentions.domain.model.MentionTriggerType

class MentionsRepositoryImplTest {

    private lateinit var localDataSource: MentionsLocalDataSource
    private lateinit var remoteDataSource: MentionsRemoteDataSource
    private lateinit var repository: MentionsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = MentionsLocalDataSource(0)
        remoteDataSource = MentionsRemoteDataSource(0)
        repository = MentionsRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() = runBlocking {
        val state = repository.getState()
        assertEquals(MentionTriggerType.NONE, state.query.triggerType)
        assertTrue(state.candidates.isEmpty())
        assertFalse(state.isSearching)
        assertFalse(state.isPanelVisible)
    }

    @Test
    fun testUpdateQuery() = runBlocking {
        val query = MentionQuery(
            triggerType = MentionTriggerType.USERNAME,
            query = "john",
            startPosition = 0,
            length = 5
        )
        repository.updateQuery(query)

        val state = repository.getState()
        assertEquals("john", state.query.query)
        assertEquals(MentionTriggerType.USERNAME, state.query.triggerType)
        assertTrue(state.isSearching)
    }

    @Test
    fun testSetCandidates() = runBlocking {
        val query = MentionQuery(
            triggerType = MentionTriggerType.USERNAME,
            query = "john",
            startPosition = 0,
            length = 5
        )
        repository.updateQuery(query)

        val candidates = listOf(
            MentionCandidate.UserCandidate(
                id = "user_1",
                userId = 1L,
                username = "john_doe",
                firstName = "John",
                lastName = "Doe"
            )
        )
        repository.setCandidates(candidates)

        val state = repository.getState()
        assertEquals(1, state.candidates.size)
        assertFalse(state.isSearching)
        assertTrue(state.isPanelVisible)
    }

    @Test
    fun testSetSearchingAndPanelVisible() = runBlocking {
        repository.setSearching(true)
        assertTrue(repository.getState().isSearching)

        repository.setPanelVisible(true)
        assertTrue(repository.getState().isPanelVisible)

        repository.setPanelVisible(false)
        assertFalse(repository.getState().isPanelVisible)
    }

    @Test
    fun testObserveState() = runBlocking {
        val candidate = MentionCandidate.HashtagCandidate("tag_tg", "tg")
        repository.setCandidates(listOf(candidate))

        val emitted = repository.observeState().first()
        assertEquals(1, emitted.candidates.size)
        assertEquals("tag_tg", emitted.candidates[0].id)
    }

    @Test
    fun testClear() = runBlocking {
        repository.updateQuery(MentionQuery(triggerType = MentionTriggerType.USERNAME, query = "test"))
        repository.setSearching(true)
        repository.setPanelVisible(true)

        repository.clear()

        val state = repository.getState()
        assertEquals(MentionTriggerType.NONE, state.query.triggerType)
        assertTrue(state.candidates.isEmpty())
        assertFalse(state.isSearching)
        assertFalse(state.isPanelVisible)
    }
}
