package org.telegram.messenger.feature.messaging.botforum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.botforum.data.datasource.BotForumLocalDataSource
import org.telegram.messenger.feature.messaging.botforum.data.datasource.BotForumRemoteDataSource
import org.telegram.messenger.feature.messaging.botforum.data.repository.BotForumRepositoryImpl
import org.telegram.messenger.feature.messaging.botforum.domain.model.StreamingSendButtonState

class BotForumRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeBotForumRemoteDataSource
    private lateinit var fakeLocalDataSource: FakeBotForumLocalDataSource
    private lateinit var repository: BotForumRepositoryImpl

    private class FakeBotForumRemoteDataSource : BotForumRemoteDataSource(0) {
        var lastStoppedRandomId: Long = 0L

        override suspend fun sendStopDraft(userId: Long, topicId: Long, randomId: Long): Result<Boolean> {
            lastStoppedRandomId = randomId
            return Result.Success(true)
        }
    }

    private class FakeBotForumLocalDataSource : BotForumLocalDataSource(0)

    @Before
    fun setup() {
        fakeRemoteDataSource = FakeBotForumRemoteDataSource()
        fakeLocalDataSource = FakeBotForumLocalDataSource()
        repository = BotForumRepositoryImpl(
            currentAccount = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testDraftUpdateAndStreamingButtonState() = runBlocking {
        val userId = 100L
        val topicId = 1
        val randomId = 999L

        assertEquals(StreamingSendButtonState.NO_STREAMING, repository.getStreamingSendButtonState(userId, topicId))
        assertFalse(repository.hasBotForumDrafts(userId, topicId))

        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = randomId,
            text = "AI is typing...",
            canStop = true,
            keepOnStop = false
        )

        assertTrue(repository.hasBotForumDrafts(userId, topicId))
        assertEquals(StreamingSendButtonState.STOP, repository.getStreamingSendButtonState(userId, topicId))

        val state = repository.observeState().first()
        assertEquals(1, state.activeDrafts.size)
        val draft = state.activeDrafts[Triple(userId, topicId, randomId)]
        assertNotNull(draft)
        assertEquals("AI is typing...", draft?.text)
        assertTrue(draft?.canStop == true)
    }

    @Test
    fun testBlockingStreamingState() {
        val userId = 101L
        val topicId = 2
        val randomId = 888L

        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = randomId,
            text = "Generating response...",
            canStop = false,
            keepOnStop = true
        )

        assertEquals(StreamingSendButtonState.BLOCKING, repository.getStreamingSendButtonState(userId, topicId))
    }

    @Test
    fun testStopStreaming() {
        val userId = 102L
        val topicId = 3
        val randomId = 777L

        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = randomId,
            text = "Streaming text...",
            canStop = true,
            keepOnStop = false
        )

        repository.stopStreaming(userId, topicId.toLong())
        assertFalse(repository.hasBotForumDrafts(userId, topicId))
        assertEquals(StreamingSendButtonState.NO_STREAMING, repository.getStreamingSendButtonState(userId, topicId))
        assertEquals(777L, fakeRemoteDataSource.lastStoppedRandomId)
    }

    @Test
    fun testCheckNewMessageDraftReplacement() {
        val userId = 103L
        val topicId = 4
        val randomId = 666L

        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = randomId,
            text = "Hello world",
            canStop = true,
            keepOnStop = false
        )

        val replaced = repository.checkNewMessageDraftReplacement(userId, topicId, "Hello world, here is the full response!")
        assertNotNull(replaced)
        assertEquals(666L, replaced?.randomId)
        assertEquals("Hello world", replaced?.text)

        // Draft should have been consumed
        assertFalse(repository.hasBotForumDrafts(userId, topicId))
        val secondCheck = repository.checkNewMessageDraftReplacement(userId, topicId, "Hello again")
        assertNull(secondCheck)
    }

    @Test
    fun testTopicStreamingPersistence() {
        val dialogId = 500L
        val topicId = 10L

        assertFalse(repository.isStreamingTopic(dialogId, topicId))
        repository.saveIsStreamingTopic(dialogId, topicId, true)
        assertTrue(repository.isStreamingTopic(dialogId, topicId))

        repository.saveIsStreamingTopic(dialogId, topicId, false)
        assertFalse(repository.isStreamingTopic(dialogId, topicId))
    }

    @Test
    fun testDraftTimeout() {
        val userId = 104L
        val topicId = 5
        val randomId = 555L

        repository.onBotDraftUpdate(userId, topicId, randomId, "Draft to timeout", canStop = true, keepOnStop = false)
        assertTrue(repository.hasBotForumDrafts(userId, topicId))

        repository.onBotDraftTimeout(userId, topicId, randomId)
        assertFalse(repository.hasBotForumDrafts(userId, topicId))
    }
}
