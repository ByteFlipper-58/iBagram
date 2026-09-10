package org.telegram.messenger.feature.messaging.chatmeta

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.messaging.chatmeta.data.repository.LegacyChatMessagesMetadataRepository
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CancelPendingMetadataRequestsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CheckMessagesMetadataUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.GetChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesExtendedMediaUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesReactionsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.ObserveChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.presentation.ChatMetadataEvent
import org.telegram.messenger.feature.messaging.chatmeta.presentation.ChatMetadataViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ChatMetadataDomainTest {

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
    fun testMessageMetadataCheckItemNeedsChecks() {
        val now = 100_000L
        val item = MessageMetadataCheckItem(
            messageId = 42,
            dialogId = -100123456L,
            canHaveReactions = true,
            hasExtendedMedia = true,
            hasStory = true,
            storyId = 777,
            lastReactionsCheckTime = now - 16_000L,       // > 15s
            lastExtendedMediaCheckTime = now - 31_000L,   // > 30s
            lastStoryCheckTime = now - 301_000L           // > 300s
        )

        assertTrue(item.needsReactionsCheck(now))
        assertTrue(item.needsExtendedMediaCheck(now))
        assertTrue(item.needsStoryCheck(now))

        val recentItem = item.copy(
            lastReactionsCheckTime = now - 5_000L,
            lastExtendedMediaCheckTime = now - 10_000L,
            lastStoryCheckTime = now - 60_000L
        )

        assertFalse(recentItem.needsReactionsCheck(now))
        assertFalse(recentItem.needsExtendedMediaCheck(now))
        assertFalse(recentItem.needsStoryCheck(now))
    }

    @Test
    fun testChatMetadataStatsModelProperties() {
        val stats = ChatMetadataStatsModel(
            activeDialogId = 12345L,
            activeReactionsRequestsCount = 2,
            activeExtendedMediaRequestsCount = 1,
            totalReactionsCheckedCount = 10L,
            totalExtendedMediaCheckedCount = 5L,
            totalStoriesCheckedCount = 2L
        )

        assertTrue(stats.hasPendingRequests)
        assertEquals(17L, stats.totalCheckedCount)

        val idleStats = ChatMetadataStatsModel()
        assertFalse(idleStats.hasPendingRequests)
        assertEquals(0L, idleStats.totalCheckedCount)
    }

    @Test
    fun testChatMetadataBatchResultProperties() {
        val emptyResult = ChatMetadataBatchResult()
        assertTrue(emptyResult.isEmpty)
        assertEquals(0, emptyResult.totalCount)

        val filledResult = ChatMetadataBatchResult(
            reactionMessageIds = listOf(1, 2),
            extendedMediaMessageIds = listOf(3),
            storyIds = listOf(10)
        )
        assertFalse(filledResult.isEmpty)
        assertEquals(4, filledResult.totalCount)
    }

    @Test
    fun testRepositoryCheckMessages() {
        val repository = LegacyChatMessagesMetadataRepository(0)
        val now = 500_000L

        val items = listOf(
            MessageMetadataCheckItem(
                messageId = 1,
                dialogId = 123L,
                canHaveReactions = true,
                lastReactionsCheckTime = 0L
            ),
            MessageMetadataCheckItem(
                messageId = 2,
                dialogId = 123L,
                hasExtendedMedia = true,
                lastExtendedMediaCheckTime = 0L
            ),
            MessageMetadataCheckItem(
                messageId = 3,
                dialogId = 123L,
                hasStory = true,
                storyId = 55,
                lastStoryCheckTime = 0L
            )
        )

        val checkRes = repository.checkMessages(123L, items, now)
        assertTrue(checkRes is Result.Success)
        val batch = (checkRes as Result.Success).data
        assertEquals(listOf(1), batch.reactionMessageIds)
        assertEquals(listOf(2), batch.extendedMediaMessageIds)
        assertEquals(listOf(55), batch.storyIds)

        val stats = repository.getStats()
        assertEquals(123L, stats.activeDialogId)
        assertEquals(1L, stats.totalReactionsCheckedCount)
        assertEquals(1L, stats.totalExtendedMediaCheckedCount)
        assertEquals(1L, stats.totalStoriesCheckedCount)

        val cancelRes = repository.cancelPendingRequests()
        assertTrue(cancelRes is Result.Success)
    }

    @Test
    fun testUseCases() = runTest(testDispatcher) {
        val repository = LegacyChatMessagesMetadataRepository(0)
        val observeUseCase = ObserveChatMetadataStatsUseCase(repository)
        val getStatsUseCase = GetChatMetadataStatsUseCase(repository)
        val checkUseCase = CheckMessagesMetadataUseCase(repository)
        val loadReactionsUseCase = LoadMessagesReactionsUseCase(repository)
        val loadExtendedMediaUseCase = LoadMessagesExtendedMediaUseCase(repository)
        val cancelUseCase = CancelPendingMetadataRequestsUseCase(repository)

        assertEquals(0L, getStatsUseCase().totalCheckedCount)

        val items = listOf(
            MessageMetadataCheckItem(messageId = 10, dialogId = 555L, canHaveReactions = true)
        )
        val result = checkUseCase(555L, items, System.currentTimeMillis())
        assertTrue(result is Result.Success)
        assertEquals(1L, getStatsUseCase().totalReactionsCheckedCount)

        val reactionsRes = loadReactionsUseCase(555L, listOf(10, 11))
        assertTrue(reactionsRes is Result.Success)

        val mediaRes = loadExtendedMediaUseCase(555L, listOf(12))
        assertTrue(mediaRes is Result.Success)

        val cancelRes = cancelUseCase()
        assertTrue(cancelRes is Result.Success)
    }

    @Test
    fun testViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyChatMessagesMetadataRepository(0)
        val viewModel = ChatMetadataViewModel(
            observeChatMetadataStatsUseCase = ObserveChatMetadataStatsUseCase(repository),
            getChatMetadataStatsUseCase = GetChatMetadataStatsUseCase(repository),
            checkMessagesMetadataUseCase = CheckMessagesMetadataUseCase(repository),
            loadMessagesReactionsUseCase = LoadMessagesReactionsUseCase(repository),
            loadMessagesExtendedMediaUseCase = LoadMessagesExtendedMediaUseCase(repository),
            cancelPendingMetadataRequestsUseCase = CancelPendingMetadataRequestsUseCase(repository)
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isChecking)

        val items = listOf(
            MessageMetadataCheckItem(messageId = 100, dialogId = 999L, canHaveReactions = true)
        )
        viewModel.onEvent(ChatMetadataEvent.CheckMessages(999L, items, System.currentTimeMillis()))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(100), viewModel.uiState.value.lastBatchResult.reactionMessageIds)
        assertEquals(1L, viewModel.uiState.value.totalChecked)

        viewModel.onEvent(ChatMetadataEvent.LoadReactions(999L, listOf(100)))
        viewModel.onEvent(ChatMetadataEvent.LoadExtendedMedia(999L, listOf(101)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ChatMetadataEvent.CancelPending)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ChatMetadataEvent.DismissInfo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun testAccountFeatureContainerIntegration() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.chatMessagesMetadataRepository)
        assertNotNull(container.observeChatMetadataStatsUseCase)
        assertNotNull(container.getChatMetadataStatsUseCase)
        assertNotNull(container.checkMessagesMetadataUseCase)
        assertNotNull(container.loadMessagesReactionsUseCase)
        assertNotNull(container.loadMessagesExtendedMediaUseCase)
        assertNotNull(container.cancelPendingMetadataRequestsUseCase)

        val vm1 = container.chatMetadataViewModel
        val vm2 = container.chatMetadataViewModel
        assertEquals(vm1, vm2)

        val createdVm = container.createChatMetadataViewModel()
        assertNotNull(createdVm)
    }
}
