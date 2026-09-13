package org.telegram.messenger.feature.security.sessions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.data.datasource.SessionsLocalDataSource
import org.telegram.messenger.feature.security.sessions.data.datasource.SessionsRemoteDataSource
import org.telegram.messenger.feature.security.sessions.data.repository.SessionsRepositoryImpl
import org.telegram.messenger.feature.security.sessions.domain.model.SessionModel
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel

class SessionsRepositoryImplTest {

    private lateinit var localDataSource: SessionsLocalDataSource
    private lateinit var remoteDataSource: SessionsRemoteDataSource
    private lateinit var repository: SessionsRepositoryImpl

    private fun createDummySession(hash: Long, isCurrent: Boolean = false): SessionModel {
        return SessionModel(
            hash = hash,
            deviceModel = "TestDevice",
            platform = "Android",
            systemVersion = "14",
            appName = "iBagram",
            appVersion = "1.0",
            dateCreated = 1000,
            dateActive = 2000,
            ip = "127.0.0.1",
            country = "Test",
            region = "Test",
            isCurrent = isCurrent,
            isOfficialApp = true,
            isPasswordPending = false,
            acceptSecretChats = true,
            acceptCalls = true,
            canAcceptSecretChats = true,
            canAcceptCalls = true,
            isUnconfirmed = false
        )
    }

    @Before
    fun setUp() {
        val testAccount = 0
        localDataSource = SessionsLocalDataSource(testAccount)
        remoteDataSource = SessionsRemoteDataSource(testAccount)
        repository = SessionsRepositoryImpl(
            currentAccount = testAccount,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testGetSessionsFromCache() = runBlocking {
        val current = createDummySession(1L, isCurrent = true)
        val other = createDummySession(2L, isCurrent = false)
        val initialList = SessionsListModel(
            currentSession = current,
            otherSessions = listOf(other),
            passwordPendingSessions = emptyList(),
            ttlDays = 30
        )
        localDataSource.putCachedSessions(initialList)

        val result = repository.getSessions()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(current, data.currentSession)
        assertEquals(1, data.otherSessions.size)
        assertEquals(2L, data.otherSessions[0].hash)
        assertEquals(30, data.ttlDays)
    }

    @Test
    fun testLocalDataOperations() = runBlocking {
        val s1 = createDummySession(10L)
        val s2 = createDummySession(20L)
        localDataSource.putCachedSessions(
            SessionsListModel(
                currentSession = createDummySession(1L, true),
                otherSessions = listOf(s1, s2)
            )
        )

        localDataSource.removeSession(10L)
        val cachedAfterRemove = localDataSource.getCachedSessions()
        assertEquals(1, cachedAfterRemove.otherSessions.size)
        assertEquals(20L, cachedAfterRemove.otherSessions[0].hash)

        localDataSource.updateSessionSettings(20L, acceptSecretChats = false, acceptCalls = false)
        val updatedSession = localDataSource.getCachedSessions().otherSessions[0]
        assertFalse(updatedSession.acceptSecretChats)
        assertFalse(updatedSession.acceptCalls)

        localDataSource.clearOtherSessions()
        assertTrue(localDataSource.getCachedSessions().otherSessions.isEmpty())

        localDataSource.setSessionsTtl(180)
        assertEquals(180, localDataSource.getCachedSessions().ttlDays)
    }

    @Test
    fun testWebSessionsLocalOperations() = runBlocking {
        val w1 = WebSessionModel(
            hash = 100L,
            botId = 55L,
            domain = "web.telegram.org",
            browser = "Chrome",
            platform = "Windows",
            dateCreated = 1000,
            dateActive = 2000,
            ip = "1.2.3.4",
            region = "RU"
        )
        localDataSource.putCachedWebSessions(listOf(w1))
        assertEquals(1, localDataSource.getCachedWebSessions().size)

        localDataSource.removeWebSession(100L)
        assertTrue(localDataSource.getCachedWebSessions().isEmpty())
    }

    @Test
    fun testDecodeTokenFromLink() {
        val link = "tg://login?token=dGVzdF90b2tlbg=="
        val tokenBytes = localDataSource.decodeTokenFromLink(link)
        assertNotNull(tokenBytes)
        val decodedStr = String(tokenBytes)
        assertEquals("test_token", decodedStr)
    }

    @Test
    fun testAcceptQrLoginEmptyTokenFails() = runBlocking {
        val result = repository.acceptQrLogin(ByteArray(0))
        assertTrue(result is Result.Failure)
    }
}
