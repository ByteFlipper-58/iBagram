package org.telegram.messenger.feature.cachebychats

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
import org.telegram.messenger.feature.cachebychats.data.repository.LegacyCacheByChatsRepository
import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaExceptionModel
import org.telegram.messenger.feature.cachebychats.domain.usecase.ClearKeepMediaExceptionsUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.GetCacheByChatsConfigUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.ObserveCacheByChatsConfigUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.RemoveKeepMediaExceptionUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.SetKeepMediaDurationUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.SetKeepMediaExceptionUseCase
import org.telegram.messenger.feature.cachebychats.presentation.CacheByChatsEvent
import org.telegram.messenger.feature.cachebychats.presentation.CacheByChatsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CacheByChatsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyCacheByChatsRepository
    private lateinit var observeCacheByChatsConfigUseCase: ObserveCacheByChatsConfigUseCase
    private lateinit var getCacheByChatsConfigUseCase: GetCacheByChatsConfigUseCase
    private lateinit var setKeepMediaDurationUseCase: SetKeepMediaDurationUseCase
    private lateinit var setKeepMediaExceptionUseCase: SetKeepMediaExceptionUseCase
    private lateinit var removeKeepMediaExceptionUseCase: RemoveKeepMediaExceptionUseCase
    private lateinit var clearKeepMediaExceptionsUseCase: ClearKeepMediaExceptionsUseCase
    private lateinit var viewModel: CacheByChatsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = LegacyCacheByChatsRepository(account = 0)
        observeCacheByChatsConfigUseCase = ObserveCacheByChatsConfigUseCase(repository)
        getCacheByChatsConfigUseCase = GetCacheByChatsConfigUseCase(repository)
        setKeepMediaDurationUseCase = SetKeepMediaDurationUseCase(repository)
        setKeepMediaExceptionUseCase = SetKeepMediaExceptionUseCase(repository)
        removeKeepMediaExceptionUseCase = RemoveKeepMediaExceptionUseCase(repository)
        clearKeepMediaExceptionsUseCase = ClearKeepMediaExceptionsUseCase(repository)

        viewModel = CacheByChatsViewModel(
            observeCacheByChatsConfigUseCase = observeCacheByChatsConfigUseCase,
            getCacheByChatsConfigUseCase = getCacheByChatsConfigUseCase,
            setKeepMediaDurationUseCase = setKeepMediaDurationUseCase,
            setKeepMediaExceptionUseCase = setKeepMediaExceptionUseCase,
            removeKeepMediaExceptionUseCase = removeKeepMediaExceptionUseCase,
            clearKeepMediaExceptionsUseCase = clearKeepMediaExceptionsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testKeepMediaDurationValues() {
        assertEquals(86_400L, KeepMediaDuration.ONE_DAY.seconds)
        assertEquals(172_800L, KeepMediaDuration.TWO_DAY.seconds)
        assertEquals(604_800L, KeepMediaDuration.ONE_WEEK.seconds)
        assertEquals(2_592_000L, KeepMediaDuration.ONE_MONTH.seconds)
        assertEquals(Long.MAX_VALUE, KeepMediaDuration.FOREVER.seconds)
        assertEquals(0L, KeepMediaDuration.DELETE.seconds)

        assertEquals(KeepMediaDuration.FOREVER, KeepMediaDuration.defaultForType(CacheChatType.USER))
        assertEquals(KeepMediaDuration.ONE_MONTH, KeepMediaDuration.defaultForType(CacheChatType.GROUP))
        assertEquals(KeepMediaDuration.ONE_WEEK, KeepMediaDuration.defaultForType(CacheChatType.CHANNEL))
        assertEquals(KeepMediaDuration.TWO_DAY, KeepMediaDuration.defaultForType(CacheChatType.STORIES))
    }

    @Test
    fun testDefaultConfigAndRetentionUpdates() {
        val config = getCacheByChatsConfigUseCase()
        assertEquals(KeepMediaDuration.FOREVER, config.userDuration)
        assertEquals(KeepMediaDuration.ONE_MONTH, config.groupDuration)
        assertEquals(KeepMediaDuration.ONE_WEEK, config.channelDuration)
        assertEquals(KeepMediaDuration.TWO_DAY, config.storiesDuration)
        assertTrue(config.exceptions.isEmpty())

        // Change group duration to 1 week
        setKeepMediaDurationUseCase(CacheChatType.GROUP, KeepMediaDuration.ONE_WEEK)
        assertEquals(KeepMediaDuration.ONE_WEEK, getCacheByChatsConfigUseCase().groupDuration)

        // Change stories duration to 1 day
        setKeepMediaDurationUseCase(CacheChatType.STORIES, KeepMediaDuration.ONE_DAY)
        assertEquals(KeepMediaDuration.ONE_DAY, getCacheByChatsConfigUseCase().storiesDuration)
    }

    @Test
    fun testExceptionsLifecycle() {
        // Add exception for user chat 1001L -> 1 day
        setKeepMediaExceptionUseCase(1001L, CacheChatType.USER, KeepMediaDuration.ONE_DAY)

        val userExceptions = repository.getExceptions(CacheChatType.USER)
        assertEquals(1, userExceptions.size)
        assertEquals(1001L, userExceptions[0].dialogId)
        assertEquals(KeepMediaDuration.ONE_DAY, userExceptions[0].duration)

        // Add exception for group chat -2002L -> delete
        setKeepMediaExceptionUseCase(-2002L, CacheChatType.GROUP, KeepMediaDuration.DELETE)
        assertEquals(2, getCacheByChatsConfigUseCase().exceptions.size)

        // Find exception
        val found = getCacheByChatsConfigUseCase().findException(1001L)
        assertNotNull(found)
        assertEquals(KeepMediaDuration.ONE_DAY, found!!.duration)

        // Update existing exception for 1001L -> forever
        setKeepMediaExceptionUseCase(1001L, CacheChatType.USER, KeepMediaDuration.FOREVER)
        assertEquals(1, repository.getExceptions(CacheChatType.USER).size)
        assertEquals(KeepMediaDuration.FOREVER, repository.getExceptions(CacheChatType.USER)[0].duration)

        // Remove single exception
        removeKeepMediaExceptionUseCase(1001L, CacheChatType.USER)
        assertTrue(repository.getExceptions(CacheChatType.USER).isEmpty())
        assertEquals(1, getCacheByChatsConfigUseCase().exceptions.size)

        // Clear all exceptions for group
        clearKeepMediaExceptionsUseCase(CacheChatType.GROUP)
        assertTrue(getCacheByChatsConfigUseCase().exceptions.isEmpty())
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CacheChatType.USER, viewModel.uiState.value.selectedTab)
        assertEquals(KeepMediaDuration.FOREVER, viewModel.uiState.value.currentTabDuration)
        assertEquals(0, viewModel.uiState.value.totalExceptionsCount)

        // Switch tab to Channel
        viewModel.onEvent(CacheByChatsEvent.SelectTab(CacheChatType.CHANNEL))
        assertEquals(CacheChatType.CHANNEL, viewModel.uiState.value.selectedTab)
        assertEquals(KeepMediaDuration.ONE_WEEK, viewModel.uiState.value.currentTabDuration)

        // Update channel duration to 1 month via VM
        viewModel.onEvent(CacheByChatsEvent.SetDuration(CacheChatType.CHANNEL, KeepMediaDuration.ONE_MONTH))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(KeepMediaDuration.ONE_MONTH, viewModel.uiState.value.currentTabDuration)
        assertNotNull(viewModel.uiState.value.infoMessage)
        assertTrue(viewModel.uiState.value.infoMessage!!.contains("CHANNEL"))

        // Add channel exception via VM
        viewModel.onEvent(CacheByChatsEvent.SetException(-100123456L, CacheChatType.CHANNEL, KeepMediaDuration.ONE_DAY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentTabExceptions.size)
        assertEquals(1, viewModel.uiState.value.totalExceptionsCount)

        // Dismiss info
        viewModel.onEvent(CacheByChatsEvent.DismissInfo)
        assertNull(viewModel.uiState.value.infoMessage)

        // Clear all exceptions
        viewModel.onEvent(CacheByChatsEvent.ClearAllExceptions(CacheChatType.CHANNEL))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.totalExceptionsCount)
    }
}
