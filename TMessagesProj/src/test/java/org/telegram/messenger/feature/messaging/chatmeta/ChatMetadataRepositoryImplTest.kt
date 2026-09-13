package org.telegram.messenger.feature.messaging.chatmeta

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.data.datasource.ChatMetadataLocalDataSource
import org.telegram.messenger.feature.messaging.chatmeta.data.datasource.ChatMetadataRemoteDataSource
import org.telegram.messenger.feature.messaging.chatmeta.data.repository.ChatMetadataRepositoryImpl
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem

class ChatMetadataRepositoryImplTest {

    private lateinit var localDataSource: ChatMetadataLocalDataSource
    private lateinit var remoteDataSource: ChatMetadataRemoteDataSource
    private lateinit var repository: ChatMetadataRepositoryImpl

    @Before
    fun setUp() {
        val testAccount = 0
        localDataSource = ChatMetadataLocalDataSource(testAccount)
        remoteDataSource = ChatMetadataRemoteDataSource(testAccount)
        repository = ChatMetadataRepositoryImpl(
            account = testAccount,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testCheckMessagesFiltersItemsByInterval() = runBlocking {
        val dialogId = 123456L
        val currentTime = 1_000_000L

        val items = listOf(
            MessageMetadataCheckItem(
                messageId = 1,
                dialogId = dialogId,
                canHaveReactions = true,
                lastReactionsCheckTime = currentTime - 20_000L // > 15s -> needs check
            ),
            MessageMetadataCheckItem(
                messageId = 2,
                dialogId = dialogId,
                canHaveReactions = true,
                lastReactionsCheckTime = currentTime - 5_000L // < 15s -> skip
            ),
            MessageMetadataCheckItem(
                messageId = 3,
                dialogId = dialogId,
                hasExtendedMedia = true,
                lastExtendedMediaCheckTime = currentTime - 40_000L // > 30s -> needs check
            ),
            MessageMetadataCheckItem(
                messageId = 4,
                dialogId = dialogId,
                hasStory = true,
                storyId = 999,
                lastStoryCheckTime = currentTime - 350_000L // > 300s -> needs check
            )
        )

        val result = repository.checkMessages(dialogId, items, currentTime)
        assertTrue(result is Result.Success)
        val batch = (result as Result.Success).data

        assertEquals(listOf(1), batch.reactionMessageIds)
        assertEquals(listOf(3), batch.extendedMediaMessageIds)
        assertEquals(listOf(999), batch.storyIds)
        assertEquals(3, batch.totalCount)
        assertFalse(batch.isEmpty)

        val stats = repository.getStats()
        assertEquals(dialogId, stats.activeDialogId)
        assertEquals(1L, stats.totalReactionsCheckedCount)
        assertEquals(1L, stats.totalExtendedMediaCheckedCount)
        assertEquals(1L, stats.totalStoriesCheckedCount)
        assertEquals(3L, stats.totalCheckedCount)
    }

    @Test
    fun testEmptyCheckMessages() = runBlocking {
        val dialogId = 555L
        val result = repository.checkMessages(dialogId, emptyList(), System.currentTimeMillis())
        assertTrue(result is Result.Success)
        val batch = (result as Result.Success).data
        assertTrue(batch.isEmpty)
        assertEquals(0, batch.totalCount)
    }

    @Test
    fun testCancelPendingRequests() = runBlocking {
        localDataSource.trackReactionsRequest(101)
        localDataSource.trackReactionsRequest(102)
        localDataSource.trackExtendedMediaRequest(201)

        val statsBefore = repository.getStats()
        assertEquals(2, statsBefore.activeReactionsRequestsCount)
        assertEquals(1, statsBefore.activeExtendedMediaRequestsCount)
        assertTrue(statsBefore.hasPendingRequests)

        val cancelResult = repository.cancelPendingRequests()
        assertTrue(cancelResult is Result.Success)

        val statsAfter = repository.getStats()
        assertEquals(0, statsAfter.activeReactionsRequestsCount)
        assertEquals(0, statsAfter.activeExtendedMediaRequestsCount)
        assertFalse(statsAfter.hasPendingRequests)
    }

    @Test
    fun testRequestQueueThrottling() = runBlocking {
        // Reactions queue bound: max 5
        for (i in 1..8) {
            localDataSource.trackReactionsRequest(i)
        }
        val stats = repository.getStats()
        assertTrue(stats.activeReactionsRequestsCount <= 5)

        // Extended media queue bound: max 10
        for (i in 1..15) {
            localDataSource.trackExtendedMediaRequest(i)
        }
        val stats2 = repository.getStats()
        assertTrue(stats2.activeExtendedMediaRequestsCount <= 10)
    }
}
