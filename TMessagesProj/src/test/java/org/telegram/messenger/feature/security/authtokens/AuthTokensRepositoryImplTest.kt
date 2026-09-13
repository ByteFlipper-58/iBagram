package org.telegram.messenger.feature.security.authtokens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.security.authtokens.data.datasource.AuthTokensLocalDataSource
import org.telegram.messenger.feature.security.authtokens.data.datasource.AuthTokensRemoteDataSource
import org.telegram.messenger.feature.security.authtokens.data.repository.AuthTokensRepositoryImpl
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel

class AuthTokensRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: AuthTokensRemoteDataSource
    private lateinit var fakeLocalDataSource: AuthTokensLocalDataSource
    private lateinit var repository: AuthTokensRepositoryImpl

    @Before
    fun setup() {
        fakeRemoteDataSource = AuthTokensRemoteDataSource(0)
        fakeLocalDataSource = AuthTokensLocalDataSource(0)
        repository = AuthTokensRepositoryImpl(
            currentAccount = 0,
            remoteDataSource = fakeRemoteDataSource,
            localDataSource = fakeLocalDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testSaveAndGetLoginToken() = runBlocking {
        val token = SavedLoginTokenModel(
            hexToken = "deadbeef1",
            futureAuthTokenBase64 = "base64token1",
            userId = 12345L
        )

        repository.saveLoginToken(token)

        val tokens = repository.getSavedLoginTokens()
        assertEquals(1, tokens.size)
        assertEquals("deadbeef1", tokens[0].hexToken)
        assertEquals(12345L, tokens[0].userId)

        val state = repository.observeState().first()
        assertEquals(1, state.loginTokens.size)
        assertEquals("deadbeef1", state.loginTokens[0].hexToken)
    }

    @Test
    fun testLoginTokenDeduplicationAndPruning() = runBlocking {
        // Add 25 tokens
        for (i in 1..25) {
            val token = SavedLoginTokenModel(
                hexToken = "hex_$i",
                futureAuthTokenBase64 = "base64_$i",
                userId = 1000L + i
            )
            repository.saveLoginToken(token)
        }

        // Must prune to max 20
        val tokens = repository.getSavedLoginTokens()
        assertEquals(20, tokens.size)
        // Most recent should be at index 0
        assertEquals("hex_25", tokens[0].hexToken)

        // Save token with same userId should update and move to front
        val updated = SavedLoginTokenModel(
            hexToken = "hex_20_updated",
            futureAuthTokenBase64 = "base64_20_updated",
            userId = 1020L
        )
        repository.saveLoginToken(updated)
        val refreshed = repository.getSavedLoginTokens()
        assertEquals(20, refreshed.size)
        assertEquals("hex_20_updated", refreshed[0].hexToken)
    }

    @Test
    fun testRemoveLoginToken() = runBlocking {
        val t1 = SavedLoginTokenModel(hexToken = "token_a", userId = 1L)
        val t2 = SavedLoginTokenModel(hexToken = "token_b", userId = 2L)
        repository.saveLoginToken(t1)
        repository.saveLoginToken(t2)

        assertEquals(2, repository.getSavedLoginTokens().size)

        repository.removeLoginToken("token_a")
        val remaining = repository.getSavedLoginTokens()
        assertEquals(1, remaining.size)
        assertEquals("token_b", remaining[0].hexToken)
    }

    @Test
    fun testLogoutTokensLifecycle() = runBlocking {
        val l1 = SavedLogoutTokenModel(hexToken = "logout_1", futureAuthTokenBase64 = "f1")
        val l2 = SavedLogoutTokenModel(hexToken = "logout_2", futureAuthTokenBase64 = "f2")

        repository.addLogoutToken(l1)
        repository.addLogoutToken(l2)

        val list = repository.getSavedLogoutTokens()
        assertEquals(2, list.size)
        assertEquals("logout_2", list[0].hexToken)

        repository.removeLogoutToken("logout_1")
        val remaining = repository.getSavedLogoutTokens()
        assertEquals(1, remaining.size)
        assertEquals("logout_2", remaining[0].hexToken)
    }

    @Test
    fun testClearTokens() = runBlocking {
        repository.saveLoginToken(SavedLoginTokenModel(hexToken = "login1", userId = 1L))
        repository.addLogoutToken(SavedLogoutTokenModel(hexToken = "logout1"))

        assertFalse(repository.getSavedLoginTokens().isEmpty())
        assertFalse(repository.getSavedLogoutTokens().isEmpty())

        repository.clearAllTokens()
        assertTrue(repository.getSavedLoginTokens().isEmpty())
        assertTrue(repository.getSavedLogoutTokens().isEmpty())

        val state = repository.observeState().first()
        assertTrue(state.loginTokens.isEmpty())
        assertTrue(state.logoutTokens.isEmpty())
    }
}
