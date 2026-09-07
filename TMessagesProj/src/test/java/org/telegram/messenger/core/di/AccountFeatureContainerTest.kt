package org.telegram.messenger.core.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.model.SavedTagModel
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AccountFeatureContainerTest {

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

    private class TestSavedMessagesRepository : SavedMessagesRepository {
        override fun observeSavedDialogs(): Flow<List<SavedDialogModel>> = emptyFlow()
        override suspend fun getSavedDialogs(): Result<List<SavedDialogModel>> = Result.success(emptyList())
        override suspend fun loadDialogs(onlyCache: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun togglePin(dialogId: Long, pinned: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDialog(dialogId: Long): Result<Unit> = Result.success(Unit)
        override fun observeSavedTags(): Flow<List<SavedTagModel>> = emptyFlow()
        override fun searchDialogs(query: String): List<SavedDialogModel> = emptyList()
    }

    @Test
    fun testAccountIsolation() {
        val container0 = AccountFeatureContainer.get(0)
        val container1 = AccountFeatureContainer.get(1)

        assertEquals(0, container0.account)
        assertEquals(1, container1.account)
        assertNotSame(container0, container1)

        // Same account returns same cached instance
        val container0Again = AccountFeatureContainer.get(0)
        assertSame(container0, container0Again)
    }

    @Test
    fun testResetAccount() {
        val container0 = AccountFeatureContainer.get(0)
        AccountFeatureContainer.reset(0)
        val container0New = AccountFeatureContainer.get(0)
        assertNotSame(container0, container0New)
    }

    @Test
    fun testResetAll() {
        val container0 = AccountFeatureContainer.get(0)
        val container1 = AccountFeatureContainer.get(1)
        AccountFeatureContainer.resetAll()

        assertNotSame(container0, AccountFeatureContainer.get(0))
        assertNotSame(container1, AccountFeatureContainer.get(1))
    }

    @Test
    fun testRepositoryOverrideAndViewModelCreation() {
        val container = AccountFeatureContainer.get(0)
        val testRepo = TestSavedMessagesRepository()
        container.savedMessagesRepository = testRepo

        assertSame(testRepo, container.savedMessagesRepository)

        val vm = container.createSavedMessagesViewModel()
        assertNotNull(vm)
        assertEquals(0, vm.account)
    }
}
