package org.telegram.messenger.feature.messaging.autodelete

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.autodelete.data.datasource.AutoDeleteLocalDataSource
import org.telegram.messenger.feature.messaging.autodelete.data.datasource.AutoDeleteRemoteDataSource
import org.telegram.messenger.feature.messaging.autodelete.data.repository.AutoDeleteRepositoryImpl
import org.telegram.messenger.feature.messaging.autodelete.domain.model.AutoDeleteTtlModel

class AutoDeleteRepositoryImplTest {

    private lateinit var localDataSource: AutoDeleteLocalDataSource
    private lateinit var remoteDataSource: AutoDeleteRemoteDataSource
    private lateinit var repository: AutoDeleteRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = AutoDeleteLocalDataSource(0)
        remoteDataSource = AutoDeleteRemoteDataSource(0)
        repository = AutoDeleteRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun observeGlobalAutoDelete_emitsInitialState() = runBlocking {
        val state = repository.observeGlobalAutoDelete().first()
        assertEquals(0, state.ttl.periodSeconds)
        assertFalse(state.isLoading)
    }

    @Test
    fun setGlobalAutoDelete_updatesStateFlow() = runBlocking {
        val setResult = repository.setGlobalAutoDelete(AutoDeleteTtlModel.ONE_DAY)
        assertTrue(setResult is Result.Success)

        val state = repository.observeGlobalAutoDelete().first()
        assertEquals(86400, state.ttl.periodSeconds)
        assertTrue(state.ttl.isEnabled)
    }

    @Test
    fun getChatAutoDelete_returnsUpdatedTtl() = runBlocking {
        repository.setChatAutoDelete(12345L, AutoDeleteTtlModel.ONE_WEEK)

        val result = repository.getChatAutoDelete(12345L)
        assertTrue(result is Result.Success)
        assertEquals(7 * 24 * 3600, (result as Result.Success).data.periodSeconds)
    }

    @Test
    fun setChatsAutoDeleteBatch_updatesMultipleChats() = runBlocking {
        val chats = listOf(101L, 102L, 103L)
        val batchResult = repository.setChatsAutoDeleteBatch(chats, AutoDeleteTtlModel.ONE_MONTH)
        assertTrue(batchResult is Result.Success)

        for (id in chats) {
            val res = repository.getChatAutoDelete(id)
            assertTrue(res is Result.Success)
            assertEquals(31 * 24 * 3600, (res as Result.Success).data.periodSeconds)
        }
    }
}
