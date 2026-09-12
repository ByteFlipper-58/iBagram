package org.telegram.messenger.feature.messaging.topics

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
import org.telegram.messenger.TopicsController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.data.datasource.TopicsLocalDataSource
import org.telegram.messenger.feature.messaging.topics.data.datasource.TopicsRemoteDataSource
import org.telegram.messenger.feature.messaging.topics.data.repository.TopicsRepositoryImpl
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class TopicsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeTopicsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeTopicsRemoteDataSource
    private lateinit var repository: TopicsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeTopicsLocalDataSource()
        fakeRemoteDataSource = FakeTopicsRemoteDataSource()
        repository = TopicsRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetTopicsSuccess() = runTest {
        val topic1 = TLRPC.TL_forumTopic().apply {
            id = 1
            title = "General"
            closed = false
            pinned = true
            unread_count = 0
        }
        val topic2 = TLRPC.TL_forumTopic().apply {
            id = 2
            title = "Announcements"
            closed = false
            pinned = false
            unread_count = 3
        }
        fakeLocalDataSource.topicsMap[-100123L] = mutableListOf(topic1, topic2)

        val result = repository.getTopics(-100123L)
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertEquals("General", list[0].title)
        assertEquals("Announcements", list[1].title)
        assertEquals(3, list[1].unreadCount)
    }

    @Test
    fun testGetTopicFound() = runTest {
        val topic = TLRPC.TL_forumTopic().apply {
            id = 42
            title = "Tech Discussions"
            closed = true
            pinned = false
        }
        fakeLocalDataSource.topicsMap[-100123L] = mutableListOf(topic)

        val result = repository.getTopic(-100123L, 42L)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(42L, data.id)
        assertEquals("Tech Discussions", data.title)
        assertTrue(data.isClosed)
        assertFalse(data.isPinned)
    }

    @Test
    fun testGetTopicNotFound() = runTest {
        val result = repository.getTopic(-100123L, 999L)
        assertTrue(result is Result.Failure)
    }

    @Test
    fun testLoadTopics() = runTest {
        val resultPreload = repository.loadTopics(-100123L, fromCache = true)
        assertTrue(resultPreload is Result.Success)
        assertEquals(1, fakeLocalDataSource.loadCalls.size)
        assertEquals(true, fakeLocalDataSource.loadCalls[0].fromCache)
        assertEquals(TopicsController.LOAD_TYPE_PRELOAD, fakeLocalDataSource.loadCalls[0].loadType)

        val resultNext = repository.loadTopics(-100123L, fromCache = false)
        assertTrue(resultNext is Result.Success)
        assertEquals(2, fakeLocalDataSource.loadCalls.size)
        assertEquals(false, fakeLocalDataSource.loadCalls[1].fromCache)
        assertEquals(TopicsController.LOAD_TYPE_LOAD_NEXT, fakeLocalDataSource.loadCalls[1].loadType)
    }

    @Test
    fun testReloadTopics() = runTest {
        val result = repository.reloadTopics(-100123L)
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.reloadedChats.contains(-100123L))
    }

    @Test
    fun testToggleCloseTopic() = runTest {
        val result = repository.toggleCloseTopic(-100123L, 42L, close = true)
        assertTrue(result is Result.Success)
        assertEquals(Triple(-100123L, 42, true), fakeLocalDataSource.lastToggleClose)
    }

    @Test
    fun testTogglePinTopic() = runTest {
        val result = repository.togglePinTopic(-100123L, 42L, pin = true)
        assertTrue(result is Result.Success)
        assertEquals(Triple(-100123L, 42, true), fakeLocalDataSource.lastPin)
    }

    @Test
    fun testToggleShowTopic() = runTest {
        val result = repository.toggleShowTopic(-100123L, 42L, show = false)
        assertTrue(result is Result.Success)
        assertEquals(Triple(-100123L, 42, false), fakeLocalDataSource.lastToggleShow)
    }

    @Test
    fun testDeleteTopics() = runTest {
        val result = repository.deleteTopics(-100123L, listOf(1L, 2L, 3L))
        assertTrue(result is Result.Success)
        assertEquals(listOf(1L, 2L, 3L), fakeLocalDataSource.lastDeletedTopics)
    }

    @Test
    fun testReorderPinnedTopics() = runTest {
        val result = repository.reorderPinnedTopics(-100123L, listOf(5L, 4L, 3L))
        assertTrue(result is Result.Success)
        assertEquals(listOf(5L, 4L, 3L), fakeLocalDataSource.lastReorderedPinned)
    }

    @Test
    fun testMarkAllReactionsAsRead() = runTest {
        val result = repository.markAllReactionsAsRead(-100123L, 42L)
        assertTrue(result is Result.Success)
        assertEquals(Pair(-100123L, 42L), fakeLocalDataSource.lastMarkReactionsRead)
    }

    @Test
    fun testGetForumUnreadCount() = runTest {
        fakeLocalDataSource.unreadCounts[-100123L] = intArrayOf(5, 2)
        val result = repository.getForumUnreadCount(-100123L)
        assertTrue(result is Result.Success)
        val unread = (result as Result.Success).data
        assertEquals(5, unread.unreadTopicsCount)
        assertEquals(2, unread.unreadMessagesCount)
    }

    @Test
    fun testStranglerHook() {
        val repo = TopicsController.getTopicsRepository(0)
        assertNotNull(repo)
    }

    // Fakes
    private class FakeTopicsLocalDataSource : TopicsLocalDataSource(0) {
        val topicsMap = mutableMapOf<Long, MutableList<TLRPC.TL_forumTopic>>()
        data class LoadCall(val chatId: Long, val fromCache: Boolean, val loadType: Int)
        val loadCalls = mutableListOf<LoadCall>()
        val reloadedChats = mutableListOf<Long>()
        var lastToggleClose: Triple<Long, Int, Boolean>? = null
        var lastPin: Triple<Long, Int, Boolean>? = null
        var lastToggleShow: Triple<Long, Int, Boolean>? = null
        var lastDeletedTopics: List<Long>? = null
        var lastReorderedPinned: List<Long>? = null
        var lastMarkReactionsRead: Pair<Long, Long>? = null
        val unreadCounts = mutableMapOf<Long, IntArray>()

        override fun getTopics(chatId: Long): List<TLRPC.TL_forumTopic> {
            return topicsMap[chatId] ?: emptyList()
        }

        override fun findTopic(chatId: Long, topicId: Long): TLRPC.TL_forumTopic? {
            return topicsMap[chatId]?.find { it.id.toLong() == topicId }
        }

        override fun loadTopics(chatId: Long, fromCache: Boolean, loadType: Int) {
            loadCalls.add(LoadCall(chatId, fromCache, loadType))
        }

        override fun reloadTopics(chatId: Long) {
            reloadedChats.add(chatId)
        }

        override fun toggleCloseTopic(chatId: Long, topicId: Int, close: Boolean) {
            lastToggleClose = Triple(chatId, topicId, close)
        }

        override fun pinTopic(chatId: Long, topicId: Int, pin: Boolean) {
            lastPin = Triple(chatId, topicId, pin)
        }

        override fun toggleShowTopic(chatId: Long, topicId: Int, show: Boolean) {
            lastToggleShow = Triple(chatId, topicId, show)
        }

        override fun deleteTopics(chatId: Long, topicIds: List<Long>) {
            lastDeletedTopics = topicIds
        }

        override fun reorderPinnedTopics(chatId: Long, topicIds: List<Long>) {
            lastReorderedPinned = topicIds
        }

        override fun markAllReactionsAsRead(chatId: Long, topicId: Long) {
            lastMarkReactionsRead = Pair(chatId, topicId)
        }

        override fun getForumUnreadCount(chatId: Long): IntArray? {
            return unreadCounts[chatId] ?: intArrayOf(0, 0)
        }
    }

    private class FakeTopicsRemoteDataSource : TopicsRemoteDataSource(0)
}
