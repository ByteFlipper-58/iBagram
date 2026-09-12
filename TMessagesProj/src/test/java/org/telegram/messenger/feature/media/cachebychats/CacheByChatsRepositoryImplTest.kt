package org.telegram.messenger.feature.media.cachebychats

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
import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.feature.media.cachebychats.data.datasource.CacheByChatsLocalDataSource
import org.telegram.messenger.feature.media.cachebychats.data.datasource.CacheByChatsRemoteDataSource
import org.telegram.messenger.feature.media.cachebychats.data.repository.CacheByChatsRepositoryImpl
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaDuration

@OptIn(ExperimentalCoroutinesApi::class)
class CacheByChatsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeCacheByChatsLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeCacheByChatsRemoteDataSource
    private lateinit var repository: CacheByChatsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeCacheByChatsLocalDataSource()
        fakeRemoteDataSource = FakeCacheByChatsRemoteDataSource()
        repository = CacheByChatsRepositoryImpl(
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
    fun testDefaultDurations() {
        val config = repository.getConfig()
        assertEquals(KeepMediaDuration.FOREVER, config.userDuration)
        assertEquals(KeepMediaDuration.ONE_MONTH, config.groupDuration)
        assertEquals(KeepMediaDuration.ONE_WEEK, config.channelDuration)
        assertEquals(KeepMediaDuration.TWO_DAY, config.storiesDuration)
        assertTrue(config.exceptions.isEmpty())
    }

    @Test
    fun testSetDurationAndObserve() = runTest {
        repository.setDuration(CacheChatType.CHANNEL, KeepMediaDuration.ONE_MONTH)
        assertEquals(KeepMediaDuration.ONE_MONTH, repository.getDuration(CacheChatType.CHANNEL))

        val config = repository.getConfig()
        assertEquals(KeepMediaDuration.ONE_MONTH, config.channelDuration)
        assertEquals(CacheByChatsController.KEEP_MEDIA_ONE_MONTH, fakeLocalDataSource.keepMediaMap[CacheChatType.CHANNEL.rawType])
    }

    @Test
    fun testSetExceptionAndGetExceptions() = runTest {
        repository.setException(12345L, CacheChatType.GROUP, KeepMediaDuration.ONE_DAY)
        val exceptions = repository.getExceptions(CacheChatType.GROUP)
        assertEquals(1, exceptions.size)
        assertEquals(12345L, exceptions[0].dialogId)
        assertEquals(CacheChatType.GROUP, exceptions[0].type)
        assertEquals(KeepMediaDuration.ONE_DAY, exceptions[0].duration)

        val config = repository.getConfig()
        assertEquals(1, config.exceptions.size)
        assertEquals(12345L, config.exceptions[0].dialogId)
    }

    @Test
    fun testRemoveException() = runTest {
        repository.setException(100L, CacheChatType.USER, KeepMediaDuration.ONE_WEEK)
        repository.setException(200L, CacheChatType.USER, KeepMediaDuration.ONE_MONTH)
        assertEquals(2, repository.getExceptions(CacheChatType.USER).size)

        repository.removeException(100L, CacheChatType.USER)
        val remaining = repository.getExceptions(CacheChatType.USER)
        assertEquals(1, remaining.size)
        assertEquals(200L, remaining[0].dialogId)
    }

    @Test
    fun testClearAllExceptions() = runTest {
        repository.setException(1L, CacheChatType.STORIES, KeepMediaDuration.DELETE)
        repository.setException(2L, CacheChatType.STORIES, KeepMediaDuration.ONE_DAY)
        assertEquals(2, repository.getExceptions(CacheChatType.STORIES).size)

        repository.clearAllExceptions(CacheChatType.STORIES)
        assertEquals(0, repository.getExceptions(CacheChatType.STORIES).size)
    }

    @Test
    fun testStranglerHook() {
        val repo = CacheByChatsController.getCacheByChatsRepository(0)
        assertNotNull(repo)
    }

    // Fakes
    private class FakeCacheByChatsLocalDataSource : CacheByChatsLocalDataSource(0) {
        val keepMediaMap = mutableMapOf<Int, Int>()
        val exceptionsMap = mutableMapOf<Int, ArrayList<CacheByChatsController.KeepMediaException>>()

        override fun getKeepMedia(type: Int): Int {
            return keepMediaMap[type] ?: CacheByChatsController.getDefault(type)
        }

        override fun setKeepMedia(type: Int, keepMedia: Int) {
            keepMediaMap[type] = keepMedia
        }

        override fun getKeepMediaExceptions(type: Int): ArrayList<CacheByChatsController.KeepMediaException> {
            return exceptionsMap[type] ?: ArrayList()
        }

        override fun saveKeepMediaExceptions(type: Int, exceptions: ArrayList<CacheByChatsController.KeepMediaException>) {
            exceptionsMap[type] = ArrayList(exceptions)
        }
    }

    private class FakeCacheByChatsRemoteDataSource : CacheByChatsRemoteDataSource(0)
}
