package org.telegram.messenger.feature.botforum

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.BotForumHelper
import org.telegram.messenger.feature.botforum.data.mapper.BotForumMapper
import org.telegram.messenger.feature.botforum.data.repository.LegacyBotForumRepository
import org.telegram.messenger.feature.botforum.domain.model.BotDraftMessageModel
import org.telegram.messenger.feature.botforum.domain.model.StreamingSendButtonState
import org.telegram.messenger.feature.botforum.domain.usecase.CheckHasBotForumDraftsUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.CheckIsStreamingTopicUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.CheckNewMessageDraftReplacementUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.DeriveTopicNameFromMessageUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.GetStreamingSendButtonStateUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.ObserveBotForumStateUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.RemoveMarkedRemovedDraftsUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.ResolveStreamingButtonStateUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.SaveIsStreamingTopicUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.StopStreamingDraftUseCase
import org.telegram.messenger.feature.botforum.domain.usecase.UpdateBotForumDraftUseCase
import org.telegram.messenger.feature.botforum.presentation.BotForumEvent
import org.telegram.messenger.feature.botforum.presentation.BotForumViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BotForumDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDeriveTopicNameFromMessageUseCase() {
        val useCase = DeriveTopicNameFromMessageUseCase()

        // Null, empty, whitespace
        assertEquals("#New Chat", useCase(null))
        assertEquals("#New Chat", useCase(""))
        assertEquals("#New Chat", useCase("   "))
        assertEquals("Custom Fallback", useCase("", fallbackTitle = "Custom Fallback"))

        // Exactly 16 chars
        val exact16 = "1234567890123456"
        assertEquals(exact16, useCase(exact16))

        // Less than 16 chars
        val shortText = "Hello AI"
        assertEquals("Hello AI", useCase(shortText))

        // More than 16 chars
        val longText = "Write a comprehensive essay on quantum computing and physics"
        val expected = longText.substring(0, 16) + "..."
        assertEquals(expected, useCase(longText))
    }

    @Test
    fun testResolveStreamingButtonStateUseCase() {
        val useCase = ResolveStreamingButtonStateUseCase()

        // Empty / null
        assertEquals(StreamingSendButtonState.NO_STREAMING, useCase(null))
        assertEquals(StreamingSendButtonState.NO_STREAMING, useCase(emptyList()))

        // All drafts removed
        val removedDraft = BotDraftMessageModel(
            userId = 100L,
            topicId = 1,
            randomId = 1000L,
            localMessageId = 10,
            text = "Thinking...",
            canStop = true,
            keepOnStop = false,
            isRemoved = true
        )
        assertEquals(StreamingSendButtonState.NO_STREAMING, useCase(listOf(removedDraft)))

        // Draft with canStop = true
        val stoppableDraft = removedDraft.copy(isRemoved = false, canStop = true)
        assertEquals(StreamingSendButtonState.STOP, useCase(listOf(stoppableDraft)))

        // Draft with canStop = false
        val blockingDraft = removedDraft.copy(isRemoved = false, canStop = false)
        assertEquals(StreamingSendButtonState.BLOCKING, useCase(listOf(blockingDraft)))
    }

    @Test
    fun testBotForumMapper() {
        // Enums
        assertEquals(
            StreamingSendButtonState.BLOCKING,
            BotForumMapper.mapStreamingButtonState(BotForumHelper.SteamingSendButtonState.BLOCKING)
        )
        assertEquals(
            StreamingSendButtonState.STOP,
            BotForumMapper.mapStreamingButtonState(BotForumHelper.SteamingSendButtonState.STOP)
        )
        assertEquals(
            StreamingSendButtonState.NO_STREAMING,
            BotForumMapper.mapStreamingButtonState(BotForumHelper.SteamingSendButtonState.NO_STREAMING)
        )
        assertEquals(
            StreamingSendButtonState.NO_STREAMING,
            BotForumMapper.mapStreamingButtonState(null)
        )

        assertEquals(
            BotForumHelper.SteamingSendButtonState.BLOCKING,
            BotForumMapper.mapToLegacyStreamingButtonState(StreamingSendButtonState.BLOCKING)
        )
        assertEquals(
            BotForumHelper.SteamingSendButtonState.STOP,
            BotForumMapper.mapToLegacyStreamingButtonState(StreamingSendButtonState.STOP)
        )
        assertEquals(
            BotForumHelper.SteamingSendButtonState.NO_STREAMING,
            BotForumMapper.mapToLegacyStreamingButtonState(StreamingSendButtonState.NO_STREAMING)
        )

        // Delete Notification
        val deleteNotification = BotForumHelper.BotForumTextDraftDeleteNotification(12345L, 2L, 42)
        val mappedDelete = BotForumMapper.mapDraftDeleteNotification(deleteNotification)
        assertEquals(12345L, mappedDelete.botUserId)
        assertEquals(2L, mappedDelete.botTopicId)
        assertEquals(42, mappedDelete.messageId)

        // Topic Create Notification
        val createNotification = BotForumHelper.BotForumTopicCreateNotification(9999L, 5)
        val mappedCreate = BotForumMapper.mapTopicCreateNotification(createNotification)
        assertEquals(9999L, mappedCreate.dialogId)
        assertEquals(5, mappedCreate.topicId)
    }

    @Test
    fun testRepositoryOperations() {
        val repository = LegacyBotForumRepository(0)
        val userId = 500L
        val topicId = 3
        val randomId = 8888L

        // Initial state
        assertFalse(repository.hasBotForumDrafts(userId, topicId))
        assertEquals(StreamingSendButtonState.NO_STREAMING, repository.getStreamingSendButtonState(userId, topicId))

        // Add draft
        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = randomId,
            text = "Here is an answer",
            canStop = true,
            keepOnStop = false
        )

        assertTrue(repository.hasBotForumDrafts(userId, topicId))
        assertEquals(StreamingSendButtonState.STOP, repository.getStreamingSendButtonState(userId, topicId))

        // Draft replacement when incoming message arrives
        val replaced = repository.checkNewMessageDraftReplacement(userId, topicId, "Here is an answer completely generated")
        assertNotNull(replaced)
        assertEquals(randomId, replaced?.randomId)
        assertFalse(repository.hasBotForumDrafts(userId, topicId))

        // Streaming topics persistence
        assertFalse(repository.isStreamingTopic(userId, topicId.toLong()))
        repository.saveIsStreamingTopic(userId, topicId.toLong(), true)
        assertTrue(repository.isStreamingTopic(userId, topicId.toLong()))

        // Stop streaming
        repository.onBotDraftUpdate(
            userId = userId,
            topicId = topicId,
            randomId = 9999L,
            text = "Streaming more...",
            canStop = true,
            keepOnStop = true
        )
        repository.stopStreaming(userId, topicId.toLong())
        // Since keepOnStop = true, draft is marked as removed
        assertFalse(repository.hasBotForumDrafts(userId, topicId))

        // Clean up removed drafts
        repository.removeAllMarkedAsRemovedMessages(userId, topicId)
        assertTrue(repository.getState().activeDrafts.isEmpty())

        // Clear all
        repository.clearAll()
        assertTrue(repository.getState().streamingTopics.isEmpty())
    }

    @Test
    fun testBotForumViewModelMviLifecycle() = runTest(testDispatcher) {
        val repository = LegacyBotForumRepository(0)
        val viewModel = BotForumViewModel(
            observeBotForumStateUseCase = ObserveBotForumStateUseCase(repository),
            getStreamingSendButtonStateUseCase = GetStreamingSendButtonStateUseCase(repository),
            checkIsStreamingTopicUseCase = CheckIsStreamingTopicUseCase(repository),
            saveIsStreamingTopicUseCase = SaveIsStreamingTopicUseCase(repository),
            stopStreamingDraftUseCase = StopStreamingDraftUseCase(repository),
            updateBotForumDraftUseCase = UpdateBotForumDraftUseCase(repository),
            removeMarkedRemovedDraftsUseCase = RemoveMarkedRemovedDraftsUseCase(repository),
            checkNewMessageDraftReplacementUseCase = CheckNewMessageDraftReplacementUseCase(repository),
            checkHasBotForumDraftsUseCase = CheckHasBotForumDraftsUseCase(repository)
        )

        val dialogId = 123456L
        val topicId = 10

        // Select topic
        viewModel.onEvent(BotForumEvent.SelectTopic(dialogId, topicId))
        testScheduler.advanceUntilIdle()

        assertEquals(dialogId, viewModel.uiState.value.selectedDialogId)
        assertEquals(topicId, viewModel.uiState.value.selectedTopicId)
        assertEquals(StreamingSendButtonState.NO_STREAMING, viewModel.uiState.value.streamingButtonState)
        assertFalse(viewModel.uiState.value.isStreamingActive)

        // Incoming draft update
        viewModel.onEvent(
            BotForumEvent.OnDraftUpdate(
                userId = dialogId,
                topicId = topicId,
                randomId = 777L,
                text = "Processing question...",
                canStop = true,
                keepOnStop = false
            )
        )
        testScheduler.advanceUntilIdle()

        assertEquals(StreamingSendButtonState.STOP, viewModel.uiState.value.streamingButtonState)
        assertEquals(1, viewModel.uiState.value.activeDraftsForCurrentTopic.size)
        assertEquals("Processing question...", viewModel.uiState.value.activeDraftsForCurrentTopic.first().text)

        // Stop streaming
        viewModel.onEvent(BotForumEvent.StopStreaming(dialogId, topicId.toLong()))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreamingActive)
        assertEquals(StreamingSendButtonState.NO_STREAMING, viewModel.uiState.value.streamingButtonState)

        // Dismiss error
        viewModel.onEvent(BotForumEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
