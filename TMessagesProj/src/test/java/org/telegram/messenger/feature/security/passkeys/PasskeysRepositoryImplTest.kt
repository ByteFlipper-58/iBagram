package org.telegram.messenger.feature.security.passkeys

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.PasskeysController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.passkeys.data.datasource.PasskeysLocalDataSource
import org.telegram.messenger.feature.security.passkeys.data.datasource.PasskeysRemoteDataSource
import org.telegram.messenger.feature.security.passkeys.data.repository.PasskeysRepositoryImpl
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeysRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakePasskeysLocalDataSource
    private lateinit var fakeRemoteDataSource: FakePasskeysRemoteDataSource
    private lateinit var repository: PasskeysRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakePasskeysLocalDataSource()
        fakeRemoteDataSource = FakePasskeysRemoteDataSource()
        repository = PasskeysRepositoryImpl(
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testIsSupportedAndMaxPasskeys() = runTest {
        assertTrue(repository.isSupported())
        assertEquals(5, repository.getMaxPasskeys())

        fakeLocalDataSource.supported = false
        fakeLocalDataSource.max = 10
        assertFalse(repository.isSupported())
        assertEquals(10, repository.getMaxPasskeys())
    }

    @Test
    fun testGetPasskeysWhenNotSupported() = runTest {
        fakeLocalDataSource.supported = false
        val result = repository.getPasskeys()

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())

        val state = repository.observePasskeys().first()
        assertFalse(state.isSupported)
        assertTrue(state.passkeys.isEmpty())
        assertFalse(state.canAddPasskey)
    }

    @Test
    fun testGetPasskeysSuccessAndCaching() = runTest {
        val passkey1 = TL_account.Passkey().apply {
            id = "pk_1"
            name = "Device 1"
            date = 1000
        }
        val passkey2 = TL_account.Passkey().apply {
            id = "pk_2"
            name = "Device 2"
            date = 2000
        }
        fakeRemoteDataSource.passkeysResponse = TL_account.Passkeys().apply {
            passkeys.add(passkey1)
            passkeys.add(passkey2)
        }

        val result = repository.getPasskeys()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertEquals("pk_1", list[0].id)
        assertEquals("pk_2", list[1].id)
        assertEquals(1, fakeRemoteDataSource.getPasskeysCallCount)

        // Cached call with force = false should not invoke remote data source again
        val cachedResult = repository.getPasskeys(force = false)
        assertTrue(cachedResult is Result.Success)
        assertEquals(1, fakeRemoteDataSource.getPasskeysCallCount)

        // Force call should invoke remote data source again
        val forcedResult = repository.getPasskeys(force = true)
        assertTrue(forcedResult is Result.Success)
        assertEquals(2, fakeRemoteDataSource.getPasskeysCallCount)
    }

    @Test
    fun testGetPasskeysFailure() = runTest {
        fakeRemoteDataSource.shouldFail = true
        val result = repository.getPasskeys()

        assertTrue(result is Result.Failure)
        assertEquals("Network error", (result as Result.Failure).error.message)
    }

    @Test
    fun testDeletePasskeySuccess() = runTest {
        val passkey1 = TL_account.Passkey().apply {
            id = "pk_1"
            name = "Device 1"
            date = 1000
        }
        fakeRemoteDataSource.passkeysResponse = TL_account.Passkeys().apply {
            passkeys.add(passkey1)
        }
        repository.getPasskeys()

        val deleteResult = repository.deletePasskey("pk_1")
        assertTrue(deleteResult is Result.Success)
        assertEquals("pk_1", fakeRemoteDataSource.deletedId)

        val state = repository.observePasskeys().first()
        assertTrue(state.passkeys.isEmpty())
    }

    @Test
    fun testDeletePasskeyFailure() = runTest {
        fakeRemoteDataSource.deleteShouldReturnFalse = true
        val result = repository.deletePasskey("pk_1")
        assertTrue(result is Result.Failure)

        fakeRemoteDataSource.deleteShouldReturnFalse = false
        fakeRemoteDataSource.shouldFail = true
        val failureResult = repository.deletePasskey("pk_1")
        assertTrue(failureResult is Result.Failure)
    }

    @Test
    fun testStranglerHook() {
        val repo = PasskeysController.getPasskeysRepository(0)
        assertNotNull(repo)
    }

    private class FakePasskeysLocalDataSource : PasskeysLocalDataSource(0) {
        var supported = true
        var max = 5

        override fun isSupported(): Boolean = supported
        override fun getMaxPasskeys(): Int = max
    }

    private class FakePasskeysRemoteDataSource : PasskeysRemoteDataSource(0) {
        var shouldFail = false
        var deleteShouldReturnFalse = false
        var getPasskeysCallCount = 0
        var deletedId: String? = null
        var passkeysResponse: TL_account.Passkeys = TL_account.Passkeys()

        override suspend fun getPasskeys(): Result<TL_account.Passkeys> {
            getPasskeysCallCount++
            if (shouldFail) {
                return Result.failure("Network error")
            }
            return Result.Success(passkeysResponse)
        }

        override suspend fun deletePasskey(id: String): Result<TLRPC.Bool> {
            deletedId = id
            if (shouldFail) {
                return Result.failure("Delete failed")
            }
            if (deleteShouldReturnFalse) {
                return Result.Success(TLRPC.TL_boolFalse())
            }
            return Result.Success(TLRPC.TL_boolTrue())
        }
    }
}
