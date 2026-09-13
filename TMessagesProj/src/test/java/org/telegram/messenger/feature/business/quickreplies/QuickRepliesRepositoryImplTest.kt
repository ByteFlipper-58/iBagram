package org.telegram.messenger.feature.business.quickreplies

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.quickreplies.data.datasource.QuickRepliesLocalDataSource
import org.telegram.messenger.feature.business.quickreplies.data.datasource.QuickRepliesRemoteDataSource
import org.telegram.messenger.feature.business.quickreplies.data.repository.QuickRepliesRepositoryImpl
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel

@OptIn(ExperimentalCoroutinesApi::class)
class QuickRepliesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val currentAccount = 0

    private lateinit var localDataSource: QuickRepliesLocalDataSource
    private lateinit var remoteDataSource: QuickRepliesRemoteDataSource
    private lateinit var repository: QuickRepliesRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = QuickRepliesLocalDataSource(currentAccount)
        remoteDataSource = QuickRepliesRemoteDataSource(currentAccount)
        repository = QuickRepliesRepositoryImpl(
            currentAccount = currentAccount,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getQuickReplies returns local replies`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "hello", order = 0, topMessageId = 10, messagesCount = 2, isSpecial = true),
            QuickReplyModel(id = 2, name = "faq", order = 1, topMessageId = 11, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val result = repository.getQuickReplies()
        assertTrue(result is Result.Success)
        val replies = (result as Result.Success).data
        assertEquals(2, replies.size)
        assertEquals("hello", replies[0].name)
        assertEquals("faq", replies[1].name)
    }

    @Test
    fun `findReplyById returns matching reply or null`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "hello", order = 0, topMessageId = 10, messagesCount = 2, isSpecial = true),
            QuickReplyModel(id = 2, name = "faq", order = 1, topMessageId = 11, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val found = repository.findReplyById(1)
        assertTrue(found is Result.Success)
        assertEquals("hello", (found as Result.Success).data?.name)

        val notFound = repository.findReplyById(99)
        assertTrue(notFound is Result.Success)
        assertNull((notFound as Result.Success).data)
    }

    @Test
    fun `findReplyByName is case-insensitive`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "Pricing", order = 0, topMessageId = 10, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val found = repository.findReplyByName("pricing")
        assertTrue(found is Result.Success)
        assertNotNull((found as Result.Success).data)
        assertEquals(1, (found as Result.Success).data?.id)
    }

    @Test
    fun `isNameBusy detects existing shortcut names`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "support", order = 0, topMessageId = 10, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val busyOther = repository.isNameBusy("support", exceptId = 2)
        assertTrue(busyOther is Result.Success)
        assertTrue((busyOther as Result.Success).data)

        val notBusySame = repository.isNameBusy("support", exceptId = 1)
        assertTrue(notBusySame is Result.Success)
        assertFalse((notBusySame as Result.Success).data)

        val notBusyNew = repository.isNameBusy("unknown", exceptId = -1)
        assertTrue(notBusyNew is Result.Success)
        assertFalse((notBusyNew as Result.Success).data)
    }

    @Test
    fun `renameReply updates reply name in local cache`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 5, name = "old_name", order = 0, topMessageId = 1, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val renameResult = repository.renameReply(5, "new_name")
        assertTrue(renameResult is Result.Success)

        val updated = repository.findReplyById(5)
        assertTrue(updated is Result.Success)
        assertEquals("new_name", (updated as Result.Success).data?.name)
    }

    @Test
    fun `reorderReplies reorders items`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "a", order = 0, topMessageId = 1, messagesCount = 1, isSpecial = false),
            QuickReplyModel(id = 2, name = "b", order = 1, topMessageId = 2, messagesCount = 1, isSpecial = false),
            QuickReplyModel(id = 3, name = "c", order = 2, topMessageId = 3, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val reorderResult = repository.reorderReplies(listOf(3, 1, 2))
        assertTrue(reorderResult is Result.Success)

        val list = (repository.getQuickReplies() as Result.Success).data
        assertEquals(3, list[0].id)
        assertEquals(1, list[1].id)
        assertEquals(2, list[2].id)
    }

    @Test
    fun `deleteReplies removes matching IDs`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 1, name = "a", order = 0, topMessageId = 1, messagesCount = 1, isSpecial = false),
            QuickReplyModel(id = 2, name = "b", order = 1, topMessageId = 2, messagesCount = 1, isSpecial = false)
        )
        localDataSource.setTestReplies(sample)

        val deleteResult = repository.deleteReplies(listOf(1))
        assertTrue(deleteResult is Result.Success)

        val remaining = (repository.getQuickReplies() as Result.Success).data
        assertEquals(1, remaining.size)
        assertEquals(2, remaining[0].id)
    }

    @Test
    fun `observeQuickReplies emits initial list`() = runTest(testDispatcher) {
        val sample = listOf(
            QuickReplyModel(id = 10, name = "greeting", order = 0, topMessageId = 1, messagesCount = 1, isSpecial = true)
        )
        localDataSource.setTestReplies(sample)

        val emitted = repository.observeQuickReplies().first()
        assertEquals(1, emitted.size)
        assertEquals(10, emitted[0].id)
    }
}
