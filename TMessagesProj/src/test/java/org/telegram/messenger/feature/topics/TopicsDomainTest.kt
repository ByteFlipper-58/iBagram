package org.telegram.messenger.feature.topics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.data.mapper.TopicMapper
import org.telegram.messenger.feature.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.topics.domain.model.TopicFilterType
import org.telegram.messenger.feature.topics.domain.model.TopicModel
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository
import org.telegram.messenger.feature.topics.domain.usecase.DeleteTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.LoadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.MarkTopicReactionsAsReadUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReloadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReorderPinnedTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleCloseTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.TogglePinTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleShowTopicUseCase
import org.telegram.messenger.feature.topics.presentation.TopicsEvent
import org.telegram.messenger.feature.topics.presentation.TopicsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class TopicsDomainTest {

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
    fun testTopicModelAndGeneralFlag() {
        val generalTopic = TopicModel(
            id = 1L,
            chatId = -100123456789L,
            title = "General",
            isClosed = false,
            isPinned = true
        )
        assertTrue(generalTopic.isGeneral)
        assertTrue(generalTopic.isPinned)
        assertFalse(generalTopic.isClosed)

        val customTopic = TopicModel(
            id = 42L,
            chatId = -100123456789L,
            title = "Android Architecture",
            isClosed = true,
            isPinned = false,
            unreadCount = 5
        )
        assertFalse(customTopic.isGeneral)
        assertTrue(customTopic.isClosed)
        assertEquals(5, customTopic.unreadCount)
    }

    @Test
    fun testTopicMapper() {
        val tlTopic = TLRPC.TL_forumTopic().apply {
            id = 100
            title = "Kotlin News"
            icon_color = 0xFF5722.toInt()
            icon_emoji_id = 1234567890L
            closed = false
            pinned = true
            hidden = false
            isShort = false
            date = 1700000000
            top_message = 500
            read_inbox_max_id = 490
            read_outbox_max_id = 495
            unread_count = 10
            unread_mentions_count = 2
            unread_reactions_count = 1
            unread_poll_votes_count = 0
            pinnedOrder = 1
            totalMessagesCount = 450
        }

        val domain = TopicMapper.toDomain(-100999L, tlTopic)
        assertNotNull(domain)
        domain?.let {
            assertEquals(100L, it.id)
            assertEquals(-100999L, it.chatId)
            assertEquals("Kotlin News", it.title)
            assertEquals(0xFF5722.toInt(), it.iconColor)
            assertEquals(1234567890L, it.iconEmojiId)
            assertFalse(it.isClosed)
            assertTrue(it.isPinned)
            assertFalse(it.isHidden)
            assertEquals(10, it.unreadCount)
            assertEquals(2, it.unreadMentionsCount)
            assertEquals(1, it.unreadReactionsCount)
            assertEquals(450, it.totalMessagesCount)
        }

        val list = TopicMapper.toDomainList(-100999L, listOf(tlTopic))
        assertEquals(1, list.size)
        assertEquals(100L, list[0].id)
    }

    @Test
    fun testTopicsUseCases() = runBlocking {
        val fakeRepo = FakeTopicsRepository()
        val getTopics = GetTopicsUseCase(fakeRepo)
        val getTopic = GetTopicUseCase(fakeRepo)
        val toggleClose = ToggleCloseTopicUseCase(fakeRepo)
        val togglePin = TogglePinTopicUseCase(fakeRepo)
        val toggleShow = ToggleShowTopicUseCase(fakeRepo)
        val deleteTopics = DeleteTopicsUseCase(fakeRepo)
        val reorderPinned = ReorderPinnedTopicsUseCase(fakeRepo)
        val markReactionsRead = MarkTopicReactionsAsReadUseCase(fakeRepo)
        val getUnread = GetForumUnreadCountUseCase(fakeRepo)

        val topicsRes = getTopics(-100L)
        assertTrue(topicsRes.isSuccess)
        assertEquals(3, topicsRes.getOrNull()?.size)

        val topicRes = getTopic(-100L, 2L)
        assertTrue(topicRes.isSuccess)
        assertEquals("Development", topicRes.getOrNull()?.title)

        // Close topic 2
        toggleClose(-100L, 2L, true)
        val closedTopic = getTopic(-100L, 2L).getOrNull()
        assertTrue(closedTopic?.isClosed == true)

        // Pin topic 3
        togglePin(-100L, 3L, true)
        val pinnedTopic = getTopic(-100L, 3L).getOrNull()
        assertTrue(pinnedTopic?.isPinned == true)

        // Delete topic 3
        deleteTopics(-100L, listOf(3L))
        val afterDelete = getTopics(-100L).getOrNull()
        assertEquals(2, afterDelete?.size)

        // Reorder
        reorderPinned(-100L, listOf(2L, 1L))

        // Mark reactions read
        markReactionsRead(-100L, 2L)

        // Unread counts
        val unreadRes = getUnread(-100L)
        assertTrue(unreadRes.isSuccess)
        assertEquals(1, unreadRes.getOrNull()?.unreadTopicsCount)
        assertEquals(7, unreadRes.getOrNull()?.unreadMessagesCount)
    }

    @Test
    fun testTopicsViewModelFilteringAndEvents() = runBlocking {
        val fakeRepo = FakeTopicsRepository()
        val viewModel = TopicsViewModel(
            observeTopicsUseCase = ObserveTopicsUseCase(fakeRepo),
            observeForumUnreadCountUseCase = ObserveForumUnreadCountUseCase(fakeRepo),
            getTopicsUseCase = GetTopicsUseCase(fakeRepo),
            getTopicUseCase = GetTopicUseCase(fakeRepo),
            loadTopicsUseCase = LoadTopicsUseCase(fakeRepo),
            reloadTopicsUseCase = ReloadTopicsUseCase(fakeRepo),
            toggleCloseTopicUseCase = ToggleCloseTopicUseCase(fakeRepo),
            togglePinTopicUseCase = TogglePinTopicUseCase(fakeRepo),
            toggleShowTopicUseCase = ToggleShowTopicUseCase(fakeRepo),
            deleteTopicsUseCase = DeleteTopicsUseCase(fakeRepo),
            reorderPinnedTopicsUseCase = ReorderPinnedTopicsUseCase(fakeRepo),
            markTopicReactionsAsReadUseCase = MarkTopicReactionsAsReadUseCase(fakeRepo),
            getForumUnreadCountUseCase = GetForumUnreadCountUseCase(fakeRepo)
        )

        // Initial state
        assertEquals(0L, viewModel.uiState.value.chatId)
        assertTrue(viewModel.uiState.value.topics.isEmpty())

        // Load topics
        viewModel.onEvent(TopicsEvent.LoadTopics(-100L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(-100L, viewModel.uiState.value.chatId)
        assertEquals(3, viewModel.uiState.value.topics.size)
        assertEquals(3, viewModel.uiState.value.filteredTopics.size)
        assertEquals(1, viewModel.uiState.value.unreadCount.unreadTopicsCount)

        // Filter: OPEN topics (topic 1 is open, topic 2 is open, topic 3 is closed)
        viewModel.onEvent(TopicsEvent.SetFilter(TopicFilterType.OPEN))
        assertEquals(2, viewModel.uiState.value.filteredTopics.size)

        // Filter: CLOSED topics
        viewModel.onEvent(TopicsEvent.SetFilter(TopicFilterType.CLOSED))
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("Random", viewModel.uiState.value.filteredTopics[0].title)

        // Filter: PINNED topics
        viewModel.onEvent(TopicsEvent.SetFilter(TopicFilterType.PINNED))
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("General", viewModel.uiState.value.filteredTopics[0].title)

        // Reset filter to ALL and Search
        viewModel.onEvent(TopicsEvent.SetFilter(TopicFilterType.ALL))
        viewModel.onEvent(TopicsEvent.Search("dev"))
        assertEquals(1, viewModel.uiState.value.filteredTopics.size)
        assertEquals("Development", viewModel.uiState.value.filteredTopics[0].title)

        // Clear Search
        viewModel.onEvent(TopicsEvent.Search(""))
        assertEquals(3, viewModel.uiState.value.filteredTopics.size)
    }

    private class FakeTopicsRepository : TopicsRepository {
        val topics = mutableListOf(
            TopicModel(id = 1L, chatId = -100L, title = "General", isClosed = false, isPinned = true, unreadCount = 0),
            TopicModel(id = 2L, chatId = -100L, title = "Development", isClosed = false, isPinned = false, unreadCount = 7),
            TopicModel(id = 3L, chatId = -100L, title = "Random", isClosed = true, isPinned = false, unreadCount = 0)
        )

        private val topicsFlow = MutableStateFlow<List<TopicModel>>(topics.toList())
        private val unreadFlow = MutableStateFlow(ForumUnreadCountModel(-100L, 1, 7))

        override fun observeTopics(chatId: Long): Flow<List<TopicModel>> = topicsFlow.asStateFlow()

        override fun observeForumUnreadCount(chatId: Long): Flow<ForumUnreadCountModel> = unreadFlow.asStateFlow()

        override suspend fun getTopics(chatId: Long): Result<List<TopicModel>> =
            Result.Success(topics.filter { it.chatId == chatId })

        override suspend fun getTopic(chatId: Long, topicId: Long): Result<TopicModel> {
            val found = topics.find { it.chatId == chatId && it.id == topicId }
            return if (found != null) Result.Success(found) else Result.Failure(AppError.Generic("Not found"))
        }

        override suspend fun loadTopics(chatId: Long, fromCache: Boolean): Result<Unit> {
            topicsFlow.value = topics.filter { it.chatId == chatId }
            return Result.Success(Unit)
        }

        override suspend fun reloadTopics(chatId: Long): Result<Unit> {
            topicsFlow.value = topics.filter { it.chatId == chatId }
            return Result.Success(Unit)
        }

        override suspend fun toggleCloseTopic(chatId: Long, topicId: Long, close: Boolean): Result<Unit> {
            val index = topics.indexOfFirst { it.chatId == chatId && it.id == topicId }
            if (index != -1) {
                topics[index] = topics[index].copy(isClosed = close)
                topicsFlow.value = topics.filter { it.chatId == chatId }
            }
            return Result.Success(Unit)
        }

        override suspend fun togglePinTopic(chatId: Long, topicId: Long, pin: Boolean): Result<Unit> {
            val index = topics.indexOfFirst { it.chatId == chatId && it.id == topicId }
            if (index != -1) {
                topics[index] = topics[index].copy(isPinned = pin)
                topicsFlow.value = topics.filter { it.chatId == chatId }
            }
            return Result.Success(Unit)
        }

        override suspend fun toggleShowTopic(chatId: Long, topicId: Long, show: Boolean): Result<Unit> {
            val index = topics.indexOfFirst { it.chatId == chatId && it.id == topicId }
            if (index != -1) {
                topics[index] = topics[index].copy(isHidden = !show)
                topicsFlow.value = topics.filter { it.chatId == chatId }
            }
            return Result.Success(Unit)
        }

        override suspend fun deleteTopics(chatId: Long, topicIds: List<Long>): Result<Unit> {
            topics.removeAll { it.chatId == chatId && it.id in topicIds }
            topicsFlow.value = topics.filter { it.chatId == chatId }
            return Result.Success(Unit)
        }

        override suspend fun reorderPinnedTopics(chatId: Long, topicIds: List<Long>): Result<Unit> =
            Result.Success(Unit)

        override suspend fun markAllReactionsAsRead(chatId: Long, topicId: Long): Result<Unit> {
            val index = topics.indexOfFirst { it.chatId == chatId && it.id == topicId }
            if (index != -1) {
                topics[index] = topics[index].copy(unreadReactionsCount = 0)
                topicsFlow.value = topics.filter { it.chatId == chatId }
            }
            return Result.Success(Unit)
        }

        override suspend fun getForumUnreadCount(chatId: Long): Result<ForumUnreadCountModel> =
            Result.Success(unreadFlow.value)
    }
}
