package org.telegram.messenger.feature.messaging.savedmessages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.SavedMessagesController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.savedmessages.data.datasource.SavedMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.savedmessages.data.datasource.SavedMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.savedmessages.data.repository.SavedMessagesRepositoryImpl
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class SavedMessagesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    // Fake local data source providing controlled in-memory dialogs
    private class FakeSavedMessagesLocalDataSource(account: Int) : SavedMessagesLocalDataSource(account) {
        val dialogs = mutableListOf<SavedMessagesController.SavedDialog>()

        override fun getCachedDialogs(): List<SavedMessagesController.SavedDialog> {
            return dialogs
        }
    }

    // Fake remote data source for simulated network calls
    private class FakeSavedMessagesRemoteDataSource(account: Int) : SavedMessagesRemoteDataSource(account) {
        var remoteResult: Result<TLRPC.messages_SavedDialogs> = Result.success(TLRPC.TL_messages_savedDialogsSlice())

        override suspend fun getSavedDialogs(
            offsetId: Int,
            offsetDate: Int,
            offsetPeer: TLRPC.InputPeer,
            limit: Int,
            hash: Long
        ): Result<TLRPC.messages_SavedDialogs> {
            return remoteResult
        }

        override suspend fun reorderPinnedSavedDialogs(order: List<Long>): Result<Boolean> {
            return Result.success(true)
        }
    }

    @Test
    fun testDIContainerWiresRepositoryAndDataSources() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.messaging.savedMessagesRemoteDataSource)
        assertNotNull(container.messaging.savedMessagesLocalDataSource)

        val repo: SavedMessagesRepository = container.createSavedMessagesRepository()
        assertNotNull(repo)
        assertTrue(repo is SavedMessagesRepositoryImpl)
    }

    @Test
    fun testGetSavedDialogsEmpty() = runTest(testDispatcher) {
        val fakeLocal = FakeSavedMessagesLocalDataSource(0)
        val fakeRemote = FakeSavedMessagesRemoteDataSource(0)
        val repo = SavedMessagesRepositoryImpl(0, fakeLocal, fakeRemote)

        val result = repo.getSavedDialogs()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun testSearchDialogsWithEmptyQuery() {
        val fakeLocal = FakeSavedMessagesLocalDataSource(0)
        val fakeRemote = FakeSavedMessagesRemoteDataSource(0)
        val repo = SavedMessagesRepositoryImpl(0, fakeLocal, fakeRemote)

        val results = repo.searchDialogs("")
        assertTrue(results.isEmpty())
    }
}
