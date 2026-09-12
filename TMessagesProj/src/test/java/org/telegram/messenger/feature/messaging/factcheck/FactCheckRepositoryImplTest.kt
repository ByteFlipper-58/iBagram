package org.telegram.messenger.feature.messaging.factcheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.FactCheckController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.factcheck.data.datasource.FactCheckLocalDataSource
import org.telegram.messenger.feature.messaging.factcheck.data.datasource.FactCheckRemoteDataSource
import org.telegram.messenger.feature.messaging.factcheck.data.repository.FactCheckRepositoryImpl
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class FactCheckRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeFactCheckLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeFactCheckRemoteDataSource
    private lateinit var repository: FactCheckRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeFactCheckLocalDataSource()
        fakeRemoteDataSource = FakeFactCheckRemoteDataSource()
        repository = FactCheckRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetFactCheckFromMemory() = runTest {
        val tl = TLRPC.TL_factCheck().apply {
            hash = 111L
            country = "US"
            text = TLRPC.TL_textWithEntities().apply { text = "Verified claim" }
        }
        fakeLocalDataSource.fakeMemoryCache[111L] = tl

        val result = repository.getFactCheck(100L, 1, 111L)
        assertNotNull(result)
        assertEquals("Verified claim", result?.text)
        assertEquals("US", result?.country)
        assertEquals(111L, result?.hash)
    }

    @Test
    fun testGetFactCheckFromDatabase() = runTest {
        val tl = TLRPC.TL_factCheck().apply {
            hash = 222L
            country = "DE"
            text = TLRPC.TL_textWithEntities().apply { text = "Database fact" }
        }
        fakeLocalDataSource.fakeDbCache[222L] = tl

        val result = repository.getFactCheck(100L, 2, 222L)
        assertNotNull(result)
        assertEquals("Database fact", result?.text)
        assertEquals("DE", result?.country)
        // Verify promoted to memory
        assertEquals(tl, fakeLocalDataSource.fakeMemoryCache[222L])
    }

    @Test
    fun testGetFactCheckNotFound() = runTest {
        val result = repository.getFactCheck(100L, 3, 999L)
        assertNull(result)
    }

    @Test
    fun testLoadFactCheckSuccess() = runTest {
        val tl = TLRPC.TL_factCheck().apply {
            hash = 333L
            country = "FR"
            text = TLRPC.TL_textWithEntities().apply { text = "Remote fact" }
        }
        fakeRemoteDataSource.factCheckToReturn = tl

        val result = repository.loadFactCheck(100L, 10)
        assertTrue(result is Result.Success)
        val domain = (result as Result.Success).data
        assertNotNull(domain)
        assertEquals("Remote fact", domain?.text)
        assertEquals("FR", domain?.country)

        // Verify stored in memory and database
        assertEquals(tl, fakeLocalDataSource.fakeMemoryCache[333L])
        assertEquals(tl, fakeLocalDataSource.fakeDbCache[333L])
        assertTrue(fakeLocalDataSource.notifyLoadedCalled)
    }

    @Test
    fun testLoadFactCheckRemoteFailure() = runTest {
        fakeRemoteDataSource.shouldFail = true

        val result = repository.loadFactCheck(100L, 11)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun testApplyFactCheckWithText() = runTest {
        val result = repository.applyFactCheck(100L, 20, "Correction note", null)
        assertTrue(result is Result.Success)
        assertEquals("Correction note", fakeRemoteDataSource.lastEditedText?.text)
        assertEquals(20, fakeRemoteDataSource.lastEditedMsgId)
        assertTrue(fakeLocalDataSource.processUpdatesCalled)
    }

    @Test
    fun testApplyFactCheckBlankTextDeletes() = runTest {
        val result = repository.applyFactCheck(100L, 21, "   ", null)
        assertTrue(result is Result.Success)
        assertEquals(21, fakeRemoteDataSource.lastDeletedMsgId)
        assertTrue(fakeLocalDataSource.processUpdatesCalled)
    }

    @Test
    fun testDeleteFactCheck() = runTest {
        val result = repository.deleteFactCheck(100L, 22)
        assertTrue(result is Result.Success)
        assertEquals(22, fakeRemoteDataSource.lastDeletedMsgId)
        assertTrue(fakeLocalDataSource.processUpdatesCalled)
    }

    @Test
    fun testGetFactCheckLimit() = runTest {
        fakeLocalDataSource.limitToReturn = 4096
        val limit = repository.getFactCheckLimit()
        assertEquals(4096, limit)
    }

    @Test
    fun testStranglerHook() {
        val repo = FactCheckController.getFactCheckRepository(0)
        assertNotNull(repo)
    }

    private class FakeFactCheckLocalDataSource : FactCheckLocalDataSource(0) {
        val fakeMemoryCache = HashMap<Long, TLRPC.TL_factCheck>()
        val fakeDbCache = HashMap<Long, TLRPC.TL_factCheck>()
        var limitToReturn = 1024
        var processUpdatesCalled = false
        var notifyLoadedCalled = false
        var peerToReturn: TLRPC.InputPeer? = TLRPC.TL_inputPeerChat().apply { chat_id = 100L }

        override fun getFactCheckFromMemory(hash: Long): TLRPC.TL_factCheck? = fakeMemoryCache[hash]

        override fun putFactCheckToMemory(hash: Long, factCheck: TLRPC.TL_factCheck) {
            fakeMemoryCache[hash] = factCheck
        }

        override suspend fun getFactCheckFromDatabase(hash: Long): TLRPC.TL_factCheck? = fakeDbCache[hash]

        override suspend fun saveFactCheckToDatabase(factCheck: TLRPC.TL_factCheck) {
            fakeDbCache[factCheck.hash] = factCheck
        }

        override suspend fun deleteFactCheckFromDatabase(hash: Long) {
            fakeDbCache.remove(hash)
            fakeMemoryCache.remove(hash)
        }

        override fun getInputPeer(dialogId: Long): TLRPC.InputPeer? = peerToReturn

        override fun getFactCheckLimit(): Int = limitToReturn

        override suspend fun processUpdates(updates: TLRPC.Updates) {
            processUpdatesCalled = true
        }

        override fun notifyFactCheckLoaded() {
            notifyLoadedCalled = true
        }
    }

    private class FakeFactCheckRemoteDataSource : FactCheckRemoteDataSource(0) {
        var shouldFail = false
        var factCheckToReturn: TLRPC.TL_factCheck? = null
        var lastEditedMsgId: Int? = null
        var lastEditedText: TLRPC.TL_textWithEntities? = null
        var lastDeletedMsgId: Int? = null

        override suspend fun getFactCheck(peer: TLRPC.InputPeer, msgId: Int): Result<TLRPC.TL_factCheck?> {
            if (shouldFail) return Result.failure("Network failure")
            return Result.Success(factCheckToReturn)
        }

        override suspend fun editFactCheck(
            peer: TLRPC.InputPeer,
            msgId: Int,
            text: TLRPC.TL_textWithEntities
        ): Result<TLRPC.Updates> {
            if (shouldFail) return Result.failure("Network failure")
            lastEditedMsgId = msgId
            lastEditedText = text
            return Result.Success(TLRPC.TL_updates())
        }

        override suspend fun deleteFactCheck(peer: TLRPC.InputPeer, msgId: Int): Result<TLRPC.Updates> {
            if (shouldFail) return Result.failure("Network failure")
            lastDeletedMsgId = msgId
            return Result.Success(TLRPC.TL_updates())
        }
    }
}
