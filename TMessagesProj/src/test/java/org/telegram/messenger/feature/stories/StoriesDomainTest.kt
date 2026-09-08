package org.telegram.messenger.feature.stories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.feature.stories.data.mapper.StoryMapper
import org.telegram.messenger.feature.stories.domain.model.PeerStoriesModel
import org.telegram.messenger.feature.stories.domain.model.StealthModeModel
import org.telegram.messenger.feature.stories.domain.model.StoryLimitModel
import org.telegram.messenger.feature.stories.domain.model.StoryModel
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository
import org.telegram.messenger.feature.stories.domain.usecase.ActivateStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.DeleteStoryUseCase
import org.telegram.messenger.feature.stories.domain.usecase.GetPeerStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.GetStoryLimitUseCase
import org.telegram.messenger.feature.stories.domain.usecase.MarkStoryAsReadUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveHiddenStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveSelfStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.RefreshStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryHiddenUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryPinUseCase
import org.telegram.messenger.feature.stories.presentation.StoriesEvent
import org.telegram.messenger.feature.stories.presentation.StoriesViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories

@OptIn(ExperimentalCoroutinesApi::class)
class StoriesDomainTest {

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
    fun testDomainModels() {
        val story = StoryModel(
            id = 101,
            dialogId = 12345L,
            date = 1700000000L,
            expireDate = 1700086400L,
            caption = "Test story caption",
            mediaPath = "/path/to/media.jpg",
            isPinned = true,
            isCloseFriends = false,
            isOut = true,
            isEdited = false,
            viewsCount = 42,
            reactionsCount = 10,
            forwardsCount = 3,
            isUnread = false
        )
        assertEquals(101, story.id)
        assertEquals(12345L, story.dialogId)
        assertEquals(1700000000L, story.date)
        assertEquals("Test story caption", story.caption)
        assertTrue(story.isPinned)
        assertFalse(story.isCloseFriends)
        assertTrue(story.isOut)
        assertEquals(42, story.viewsCount)

        val peerStories = PeerStoriesModel(
            dialogId = 12345L,
            maxReadId = 100,
            stories = listOf(story),
            hasUnread = false,
            lastStoryDate = 1700000000L
        )
        assertEquals(12345L, peerStories.dialogId)
        assertEquals(1, peerStories.stories.size)
        assertFalse(peerStories.hasUnread)

        val stealthMode = StealthModeModel(
            activeUntilDate = 1700050000L,
            cooldownUntilDate = 1700100000L,
            isActive = true,
            canActivateFuture = false
        )
        assertTrue(stealthMode.isActive)
        assertFalse(stealthMode.canActivateFuture)

        val limit = StoryLimitModel(limit = 10, currentCount = 5)
        assertEquals(10, limit.limit)
        assertEquals(5, limit.currentCount)
        assertFalse(limit.isOverLimit)

        val overLimit = StoryLimitModel(limit = 3, currentCount = 3)
        assertTrue(overLimit.isOverLimit)
    }

    @Test
    fun testStoryMapper() {
        val item = TL_stories.TL_storyItem().apply {
            id = 202
            dialogId = 98765L
            date = 1695000000
            expire_date = 1695086400
            caption = "Mapped story caption"
            pinned = true
            close_friends = true
            out = false
            edited = true
            views = TL_stories.TL_storyViews().apply {
                views_count = 120
                reactions_count = 15
                forwards_count = 8
            }
        }

        val mapped = StoryMapper.mapStoryItem(item, fallbackDialogId = 0L, maxReadId = 200)
        assertEquals(202, mapped.id)
        assertEquals(98765L, mapped.dialogId)
        assertEquals(1695000000L, mapped.date)
        assertEquals("Mapped story caption", mapped.caption)
        assertTrue(mapped.isPinned)
        assertTrue(mapped.isCloseFriends)
        assertFalse(mapped.isOut)
        assertTrue(mapped.isEdited)
        assertEquals(120, mapped.viewsCount)
        assertEquals(15, mapped.reactionsCount)
        assertEquals(8, mapped.forwardsCount)
        assertTrue(mapped.isUnread) // 202 > 200 && !out

        val peerStories = TL_stories.TL_peerStories().apply {
            peer = TLRPC.TL_peerUser().apply { user_id = 98765L }
            max_read_id = 200
            stories = arrayListOf(item)
        }
        val mappedPeerStories = StoryMapper.mapPeerStories(peerStories)
        assertEquals(98765L, mappedPeerStories.dialogId)
        assertEquals(200, mappedPeerStories.maxReadId)
        assertEquals(1, mappedPeerStories.stories.size)
        assertTrue(mappedPeerStories.hasUnread)

        val stealth = TL_stories.TL_storiesStealthMode().apply {
            active_until_date = 1700050000
            cooldown_until_date = 1700100000
        }
        val mappedStealth = StoryMapper.mapStealthMode(stealth, currentTime = 1700000000)
        assertTrue(mappedStealth.isActive)
        assertFalse(mappedStealth.canActivateFuture)

        val nullStealth = StoryMapper.mapStealthMode(null)
        assertFalse(nullStealth.isActive)
        assertTrue(nullStealth.canActivateFuture)
    }

    @Test
    fun testUseCasesAndRepository() = runTest {
        val fakeRepo = FakeStoriesRepository()

        val observeStoriesUseCase = ObserveStoriesUseCase(fakeRepo)
        val observeHiddenStoriesUseCase = ObserveHiddenStoriesUseCase(fakeRepo)
        val observeStealthModeUseCase = ObserveStealthModeUseCase(fakeRepo)
        val observeSelfStoriesUseCase = ObserveSelfStoriesUseCase(fakeRepo)
        val getPeerStoriesUseCase = GetPeerStoriesUseCase(fakeRepo)
        val markStoryAsReadUseCase = MarkStoryAsReadUseCase(fakeRepo)
        val deleteStoryUseCase = DeleteStoryUseCase(fakeRepo)
        val toggleStoryPinUseCase = ToggleStoryPinUseCase(fakeRepo)
        val toggleStoryHiddenUseCase = ToggleStoryHiddenUseCase(fakeRepo)
        val activateStealthModeUseCase = ActivateStealthModeUseCase(fakeRepo)
        val getStoryLimitUseCase = GetStoryLimitUseCase(fakeRepo)
        val refreshStoriesUseCase = RefreshStoriesUseCase(fakeRepo)

        val initialStories = observeStoriesUseCase().first()
        assertEquals(1, initialStories.size)
        assertEquals(111L, initialStories[0].dialogId)

        val hiddenStories = observeHiddenStoriesUseCase().first()
        assertEquals(0, hiddenStories.size)

        val stealthMode = observeStealthModeUseCase().first()
        assertFalse(stealthMode.isActive)

        val selfStories = observeSelfStoriesUseCase().first()
        assertNotNull(selfStories)
        assertEquals(999L, selfStories?.dialogId)

        val peerStoriesResult = getPeerStoriesUseCase(111L)
        assertTrue(peerStoriesResult is Result.Success)
        val storiesList = (peerStoriesResult as Result.Success).data
        assertEquals(1, storiesList.size)
        assertEquals(555, storiesList[0].id)

        val readResult = markStoryAsReadUseCase(111L, 555)
        assertTrue(readResult is Result.Success)
        assertTrue(fakeRepo.markedRead.contains(Pair(111L, 555)))

        val pinResult = toggleStoryPinUseCase(111L, 555, true)
        assertTrue(pinResult is Result.Success)
        assertTrue(fakeRepo.pinnedStories.contains(555))

        val hideResult = toggleStoryHiddenUseCase(111L, true)
        assertTrue(hideResult is Result.Success)
        assertTrue(fakeRepo.hiddenDialogs.contains(111L))

        val stealthResult = activateStealthModeUseCase(future = true, past = true)
        assertTrue(stealthResult is Result.Success)
        assertTrue(fakeRepo.stealthActivated)

        val limitResult = getStoryLimitUseCase()
        assertTrue(limitResult is Result.Success)
        assertEquals(10, (limitResult as Result.Success).data.limit)

        val deleteResult = deleteStoryUseCase(111L, 555)
        assertTrue(deleteResult is Result.Success)
        assertTrue(fakeRepo.deletedStories.contains(555))

        val refreshResult = refreshStoriesUseCase()
        assertTrue(refreshResult is Result.Success)
        assertTrue(fakeRepo.refreshed)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest {
        val fakeRepo = FakeStoriesRepository()
        val viewModel = StoriesViewModel(
            observeStoriesUseCase = ObserveStoriesUseCase(fakeRepo),
            observeHiddenStoriesUseCase = ObserveHiddenStoriesUseCase(fakeRepo),
            observeStealthModeUseCase = ObserveStealthModeUseCase(fakeRepo),
            observeSelfStoriesUseCase = ObserveSelfStoriesUseCase(fakeRepo),
            markStoryAsReadUseCase = MarkStoryAsReadUseCase(fakeRepo),
            deleteStoryUseCase = DeleteStoryUseCase(fakeRepo),
            toggleStoryPinUseCase = ToggleStoryPinUseCase(fakeRepo),
            toggleStoryHiddenUseCase = ToggleStoryHiddenUseCase(fakeRepo),
            activateStealthModeUseCase = ActivateStealthModeUseCase(fakeRepo),
            getStoryLimitUseCase = GetStoryLimitUseCase(fakeRepo),
            refreshStoriesUseCase = RefreshStoriesUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.peerStories.size)
        assertEquals(111L, state.peerStories[0].dialogId)
        assertNotNull(state.selfStories)
        assertEquals(999L, state.selfStories?.dialogId)
        assertNotNull(state.storyLimit)
        assertEquals(10, state.storyLimit?.limit)

        viewModel.onEvent(StoriesEvent.MarkAsRead(dialogId = 111L, storyId = 555))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.markedRead.contains(Pair(111L, 555)))

        viewModel.onEvent(StoriesEvent.TogglePin(dialogId = 111L, storyId = 555, pin = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.pinnedStories.contains(555))

        viewModel.onEvent(StoriesEvent.ToggleHide(dialogId = 111L, hide = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.hiddenDialogs.contains(111L))

        viewModel.onEvent(StoriesEvent.ActivateStealthMode(future = true, past = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.stealthActivated)

        viewModel.onEvent(StoriesEvent.DeleteStory(dialogId = 111L, storyId = 555))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.deletedStories.contains(555))

        viewModel.onEvent(StoriesEvent.Refresh)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(fakeRepo.refreshed)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeStoriesRepository : StoriesRepository {
        val sampleStory = StoryModel(id = 555, dialogId = 111L, date = 1700000000L, caption = "Sample")
        val samplePeer = PeerStoriesModel(dialogId = 111L, stories = listOf(sampleStory))
        val selfPeer = PeerStoriesModel(dialogId = 999L, stories = listOf(sampleStory.copy(dialogId = 999L, id = 777)))

        val storiesFlow = MutableStateFlow(listOf(samplePeer))
        val hiddenStoriesFlow = MutableStateFlow<List<PeerStoriesModel>>(emptyList())
        val stealthModeFlow = MutableStateFlow(StealthModeModel())
        val selfStoriesFlow = MutableStateFlow<PeerStoriesModel?>(selfPeer)

        val markedRead = mutableSetOf<Pair<Long, Int>>()
        val pinnedStories = mutableSetOf<Int>()
        val hiddenDialogs = mutableSetOf<Long>()
        val deletedStories = mutableSetOf<Int>()
        var stealthActivated = false
        var refreshed = false

        override fun observeStories(): Flow<List<PeerStoriesModel>> = storiesFlow
        override fun observeHiddenStories(): Flow<List<PeerStoriesModel>> = hiddenStoriesFlow
        override fun observeStealthMode(): Flow<StealthModeModel> = stealthModeFlow
        override fun observeSelfStories(): Flow<PeerStoriesModel?> = selfStoriesFlow

        override suspend fun getStories(dialogId: Long): Result<List<StoryModel>> {
            return Result.Success(listOf(sampleStory))
        }

        override suspend fun markStoryAsRead(dialogId: Long, storyId: Int): Result<Unit> {
            markedRead.add(Pair(dialogId, storyId))
            return Result.Success(Unit)
        }

        override suspend fun deleteStory(dialogId: Long, storyId: Int): Result<Unit> {
            deletedStories.add(storyId)
            return Result.Success(Unit)
        }

        override suspend fun toggleStoryPin(dialogId: Long, storyId: Int, pin: Boolean): Result<Unit> {
            if (pin) pinnedStories.add(storyId) else pinnedStories.remove(storyId)
            return Result.Success(Unit)
        }

        override suspend fun toggleStoryHidden(dialogId: Long, hide: Boolean): Result<Unit> {
            if (hide) hiddenDialogs.add(dialogId) else hiddenDialogs.remove(dialogId)
            return Result.Success(Unit)
        }

        override suspend fun activateStealthMode(future: Boolean, past: Boolean): Result<Unit> {
            stealthActivated = true
            stealthModeFlow.value = StealthModeModel(isActive = true, canActivateFuture = false)
            return Result.Success(Unit)
        }

        override suspend fun getStoryLimit(): Result<StoryLimitModel> {
            return Result.Success(StoryLimitModel(limit = 10, currentCount = 2))
        }

        override suspend fun refreshStories(): Result<Unit> {
            refreshed = true
            return Result.Success(Unit)
        }
    }
}
