package org.telegram.messenger.feature.security.botguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.security.botguard.data.datasource.BotGuardLocalDataSource
import org.telegram.messenger.feature.security.botguard.data.datasource.BotGuardRemoteDataSource
import org.telegram.messenger.feature.security.botguard.data.repository.BotGuardRepositoryImpl
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionStatus

class BotGuardRepositoryImplTest {

    private lateinit var localDataSource: BotGuardLocalDataSource
    private lateinit var remoteDataSource: BotGuardRemoteDataSource
    private lateinit var repository: BotGuardRepositoryImpl

    @Before
    fun setUp() {
        val testAccount = 0
        localDataSource = BotGuardLocalDataSource(testAccount)
        remoteDataSource = BotGuardRemoteDataSource(testAccount)
        repository = BotGuardRepositoryImpl(
            currentAccount = testAccount,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testRegisterSessionAndState() = runBlocking {
        val dialogId = 12345L
        val botId = 67890L
        val queryId = 111L

        val session = repository.registerSession(dialogId, botId, queryId, true)
        assertNotNull(session)
        assertEquals(dialogId, session.dialogId)
        assertEquals(botId, session.guardBotId)
        assertEquals(queryId, session.queryId)
        assertTrue(session.isConfirmed)

        val fetched = repository.getSession(queryId)
        assertNotNull(fetched)
        assertEquals(session, fetched)

        val state = repository.getCurrentState()
        assertEquals(1, state.activeSessions.size)
        assertEquals(session, state.activeSessions[queryId])

        val allSessions = repository.getAllActiveSessions()
        assertEquals(1, allSessions.size)
    }

    @Test
    fun testRemoveAndClearSessions() = runBlocking {
        repository.registerSession(1L, 2L, 100L, false)
        repository.registerSession(1L, 2L, 200L, true)

        assertEquals(2, repository.getAllActiveSessions().size)

        val removed = repository.removeSession(100L)
        assertNotNull(removed)
        assertEquals(100L, removed?.queryId)
        assertEquals(1, repository.getAllActiveSessions().size)

        repository.clearAllSessions()
        assertEquals(0, repository.getAllActiveSessions().size)
        assertNull(repository.getSession(200L))
        assertTrue(repository.getCurrentState().activeSessions.isEmpty())
    }

    @Test
    fun testConfirmationAndWhitelist() = runBlocking {
        val botId = 99999L
        assertFalse(repository.isConfirmationShown(botId))

        repository.setConfirmationShown(botId, true)
        assertTrue(repository.isConfirmationShown(botId))

        repository.setConfirmationShown(botId, false)
        assertFalse(repository.isConfirmationShown(botId))

        assertFalse(repository.isBotWhitelisted(botId))
        localDataSource.setBotWhitelistedInMemory(botId, true)
        assertTrue(repository.isBotWhitelisted(botId))
    }

    @Test
    fun testPostDecision() = runBlocking {
        val decision = BotGuardDecisionResult(
            dialogId = 123L,
            guardBotId = 456L,
            queryId = 789L,
            status = BotGuardDecisionStatus.Approved
        )

        repository.postDecision(decision)

        val state = repository.getCurrentState()
        assertEquals(decision, state.lastDecision)
        assertEquals(BotGuardDecisionStatus.Approved, state.lastDecision?.status)
    }
}
