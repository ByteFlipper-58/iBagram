package org.telegram.messenger.feature.messaging.folders

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
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.folders.data.datasource.FoldersLocalDataSource
import org.telegram.messenger.feature.messaging.folders.data.datasource.FoldersRemoteDataSource
import org.telegram.messenger.feature.messaging.folders.data.repository.FoldersRepositoryImpl
import org.telegram.messenger.feature.messaging.folders.domain.model.FolderModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector

@OptIn(ExperimentalCoroutinesApi::class)
class FoldersRepositoryImplTest {

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

    // Fake local data source with controllable in-memory filter storage
    private class FakeFoldersLocalDataSource(account: Int) : FoldersLocalDataSource(account) {
        val filters = mutableListOf<MessagesController.DialogFilter>()

        override fun getRawDialogFilters(): List<MessagesController.DialogFilter> {
            return ArrayList(filters)
        }

        override fun getRawDialogFilter(id: Int): MessagesController.DialogFilter? {
            return filters.firstOrNull { it.id == id }
        }

        override fun getRawSuggestedFilters(): List<TLRPC.TL_dialogFilterSuggested> {
            return emptyList()
        }

        override suspend fun saveDialogFilter(
            filter: MessagesController.DialogFilter,
            isNew: Boolean
        ): Result<Unit> {
            val idx = filters.indexOfFirst { it.id == filter.id }
            if (idx >= 0) {
                filters[idx] = filter
            } else {
                filters.add(filter)
            }
            return Result.success(Unit)
        }

        override suspend fun deleteDialogFilter(filter: MessagesController.DialogFilter): Result<Unit> {
            filters.removeAll { it.id == filter.id }
            return Result.success(Unit)
        }

        override suspend fun saveDialogFiltersOrder(): Result<Unit> {
            return Result.success(Unit)
        }
    }

    // Fake remote data source for simulated MTProto RPC operations
    private class FakeFoldersRemoteDataSource(account: Int) : FoldersRemoteDataSource(account) {
        override suspend fun getDialogFilters(): Result<TLObject> {
            return Result.success(TLRPC.TL_messages_dialogFilters())
        }

        override suspend fun updateDialogFilter(
            id: Int,
            filter: TLRPC.TL_dialogFilter?
        ): Result<Boolean> {
            return Result.success(true)
        }

        override suspend fun updateDialogFiltersOrder(order: List<Int>): Result<Boolean> {
            return Result.success(true)
        }

        override suspend fun getSuggestedDialogFilters(): Result<Vector<TLRPC.TL_dialogFilterSuggested>> {
            return Result.success(Vector { _, _, _ -> null })
        }
    }

    @Test
    fun testDIContainerWiresFoldersDataSourcesAndRepository() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.messaging.foldersRemoteDataSource)
        assertNotNull(container.messaging.foldersLocalDataSource)

        val repo: FoldersRepository = container.createFoldersRepository()
        assertNotNull(repo)
        assertTrue(repo is FoldersRepositoryImpl)
    }

    @Test
    fun testGetFoldersEmpty() = runTest(testDispatcher) {
        val fakeLocal = FakeFoldersLocalDataSource(0)
        val fakeRemote = FakeFoldersRemoteDataSource(0)
        val repo = FoldersRepositoryImpl(0, fakeLocal, fakeRemote)

        val folders = repo.getFolders()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun testCreateAndGetFolder() = runTest(testDispatcher) {
        val fakeLocal = FakeFoldersLocalDataSource(0)
        val fakeRemote = FakeFoldersRemoteDataSource(0)
        val repo = FoldersRepositoryImpl(0, fakeLocal, fakeRemote)

        val createResult = repo.createFolder(
            name = "Work",
            flags = MessagesController.DIALOG_FILTER_FLAG_GROUPS,
            includedPeerIds = listOf(1001L, 1002L),
            excludedPeerIds = listOf(2001L),
            pinnedPeerIds = listOf(1001L),
            color = 1
        )

        assertTrue(createResult.isSuccess)
        val created = createResult.getOrNull()
        assertNotNull(created)
        assertEquals("Work", created?.name)
        assertEquals(1, created?.color)
        assertTrue(created?.includedPeerIds?.contains(1001L) == true)

        val fetched = repo.getFolder(created!!.id)
        assertNotNull(fetched)
        assertEquals("Work", fetched?.name)
    }

    @Test
    fun testUpdateFolder() = runTest(testDispatcher) {
        val fakeLocal = FakeFoldersLocalDataSource(0)
        val fakeRemote = FakeFoldersRemoteDataSource(0)
        val repo = FoldersRepositoryImpl(0, fakeLocal, fakeRemote)

        val created = repo.createFolder(name = "Initial").getOrNull()!!
        val updatedModel = created.copy(name = "Renamed")

        val updateResult = repo.updateFolder(updatedModel)
        assertTrue(updateResult.isSuccess)

        val fetched = repo.getFolder(created.id)
        assertEquals("Renamed", fetched?.name)
    }

    @Test
    fun testDeleteFolder() = runTest(testDispatcher) {
        val fakeLocal = FakeFoldersLocalDataSource(0)
        val fakeRemote = FakeFoldersRemoteDataSource(0)
        val repo = FoldersRepositoryImpl(0, fakeLocal, fakeRemote)

        val created = repo.createFolder(name = "ToDelete").getOrNull()!!
        val deleteResult = repo.deleteFolder(created.id)
        assertTrue(deleteResult.isSuccess)

        val fetched = repo.getFolder(created.id)
        assertNull(fetched)
    }

    @Test
    fun testReorderFolders() = runTest(testDispatcher) {
        val fakeLocal = FakeFoldersLocalDataSource(0)
        val fakeRemote = FakeFoldersRemoteDataSource(0)
        val repo = FoldersRepositoryImpl(0, fakeLocal, fakeRemote)

        val f1 = repo.createFolder(name = "First").getOrNull()!!
        val f2 = repo.createFolder(name = "Second").getOrNull()!!

        val reorderResult = repo.reorderFolders(listOf(f2.id, f1.id))
        assertTrue(reorderResult.isSuccess)
    }
}
