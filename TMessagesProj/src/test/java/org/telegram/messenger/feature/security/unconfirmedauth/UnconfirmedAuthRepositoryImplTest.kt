package org.telegram.messenger.feature.security.unconfirmedauth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.data.datasource.UnconfirmedAuthLocalDataSource
import org.telegram.messenger.feature.security.unconfirmedauth.data.datasource.UnconfirmedAuthRemoteDataSource
import org.telegram.messenger.feature.security.unconfirmedauth.data.repository.UnconfirmedAuthRepositoryImpl
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthModel

@OptIn(ExperimentalCoroutinesApi::class)
class UnconfirmedAuthRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: UnconfirmedAuthLocalDataSource
    private lateinit var remoteDataSource: FakeUnconfirmedAuthRemoteDataSource
    private lateinit var repository: UnconfirmedAuthRepositoryImpl

    private class FakeUnconfirmedAuthRemoteDataSource : UnconfirmedAuthRemoteDataSource(0) {
        var userAuthConfirmed: Boolean = true
        var userAuthDenied: Boolean = true
        var botAuthConfirmed: Boolean = true
        var botAuthDenied: Boolean = true

        override suspend fun confirmUserAuth(hash: Long): Result<Boolean> {
            return Result.Success(userAuthConfirmed)
        }

        override suspend fun denyUserAuth(hash: Long): Result<Boolean> {
            return Result.Success(userAuthDenied)
        }

        override suspend fun confirmBotAuth(botId: Long): Result<Boolean> {
            return Result.Success(botAuthConfirmed)
        }

        override suspend fun denyBotAuth(botId: Long): Result<Boolean> {
            return Result.Success(botAuthDenied)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = UnconfirmedAuthLocalDataSource(
            currentAccount = 0,
            ioDispatcher = testDispatcher
        )
        remoteDataSource = FakeUnconfirmedAuthRemoteDataSource()
        repository = UnconfirmedAuthRepositoryImpl(
            currentAccount = 0,
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetEmptyUnconfirmedAuths() = runTest {
        val state = repository.getUnconfirmedAuths()
        assertTrue(state.auths.isEmpty())
        assertFalse(state.hasPendingAuths)
    }

    @Test
    fun testConfirmUserAuthSuccess() = runTest {
        val auth = UnconfirmedAuthModel(
            hash = 12345L,
            date = 1000,
            device = "Pixel 8",
            location = "Berlin, Germany",
            isBot = false
        )
        localDataSource.addInMemoryFallback(auth)

        val confirmResult = repository.confirmAuth(12345L)
        assertTrue(confirmResult is Result.Success)
        assertTrue((confirmResult as Result.Success).data)

        val stateAfter = repository.getUnconfirmedAuths()
        assertTrue(stateAfter.auths.isEmpty())
    }

    @Test
    fun testDenyBotAuthSuccess() = runTest {
        val botAuth = UnconfirmedAuthModel(
            hash = 999L,
            date = 2000,
            device = "Telegram Web",
            location = "Amsterdam, Netherlands",
            isBot = true,
            botId = 777L
        )
        localDataSource.addInMemoryFallback(botAuth)

        val denyResult = repository.denyAuth(999L)
        assertTrue(denyResult is Result.Success)
        assertTrue((denyResult as Result.Success).data)

        val stateAfter = repository.getUnconfirmedAuths()
        assertTrue(stateAfter.auths.isEmpty())
    }

    @Test
    fun testConfirmAll() = runTest {
        val auth1 = UnconfirmedAuthModel(hash = 1L, date = 100, device = "Device 1", location = "Loc 1")
        val auth2 = UnconfirmedAuthModel(hash = 2L, date = 101, device = "Device 2", location = "Loc 2")
        localDataSource.addInMemoryFallback(auth1)
        localDataSource.addInMemoryFallback(auth2)

        val result = repository.confirmAll()
        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data)

        val stateAfter = repository.getUnconfirmedAuths()
        assertTrue(stateAfter.auths.isEmpty())
    }

    @Test
    fun testClearAll() = runTest {
        val auth = UnconfirmedAuthModel(hash = 42L, date = 500, device = "Device", location = "Loc")
        localDataSource.addInMemoryFallback(auth)

        val clearResult = repository.clear()
        assertTrue(clearResult is Result.Success)

        val stateAfter = repository.getUnconfirmedAuths()
        assertTrue(stateAfter.auths.isEmpty())
    }
}
