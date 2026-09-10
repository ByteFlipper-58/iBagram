package org.telegram.messenger.feature.media.sharedmedia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.sharedmedia.data.mapper.SharedMediaMapper
import org.telegram.messenger.feature.media.sharedmedia.data.repository.LegacySharedMediaRepository
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaFilterType
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaItem
import org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabType
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.CalculateMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ClearMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.FilterSharedMediaUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GetSharedMediaStateUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GroupMediaByMonthUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ObserveSharedMediaStateUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ResolveAvailableTabsUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.SelectSharedMediaTabUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.SetSharedMediaFilterUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ToggleMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.presentation.SharedMediaEvent
import org.telegram.messenger.feature.media.sharedmedia.presentation.SharedMediaViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SharedMediaDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private val resolveAvailableTabsUseCase = ResolveAvailableTabsUseCase()
    private val filterSharedMediaUseCase = FilterSharedMediaUseCase()
    private val groupMediaByMonthUseCase = GroupMediaByMonthUseCase()
    private val calculateMediaSelectionUseCase = CalculateMediaSelectionUseCase()

    private lateinit var repository: LegacySharedMediaRepository
    private lateinit var observeSharedMediaStateUseCase: ObserveSharedMediaStateUseCase
    private lateinit var getSharedMediaStateUseCase: GetSharedMediaStateUseCase
    private lateinit var selectSharedMediaTabUseCase: SelectSharedMediaTabUseCase
    private lateinit var setSharedMediaFilterUseCase: SetSharedMediaFilterUseCase
    private lateinit var toggleMediaSelectionUseCase: ToggleMediaSelectionUseCase
    private lateinit var clearMediaSelectionUseCase: ClearMediaSelectionUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacySharedMediaRepository(
            groupMediaByMonthUseCase = groupMediaByMonthUseCase,
            calculateMediaSelectionUseCase = calculateMediaSelectionUseCase,
            filterSharedMediaUseCase = filterSharedMediaUseCase
        )
        observeSharedMediaStateUseCase = ObserveSharedMediaStateUseCase(repository)
        getSharedMediaStateUseCase = GetSharedMediaStateUseCase(repository)
        selectSharedMediaTabUseCase = SelectSharedMediaTabUseCase(repository)
        setSharedMediaFilterUseCase = SetSharedMediaFilterUseCase(repository)
        toggleMediaSelectionUseCase = ToggleMediaSelectionUseCase(repository)
        clearMediaSelectionUseCase = ClearMediaSelectionUseCase(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testResolveAvailableTabs() {
        // 1. Обычный пользовательский чат со всеми медиа
        val countsNormal = mapOf(
            SharedMediaTabType.PHOTO_VIDEO to 15,
            SharedMediaTabType.FILES to 3,
            SharedMediaTabType.LINKS to 7,
            SharedMediaTabType.AUDIO to 2,
            SharedMediaTabType.VOICE to 4,
            SharedMediaTabType.GIF to 1,
            SharedMediaTabType.POLLS to 1
        )
        val tabsNormal = resolveAvailableTabsUseCase(
            dialogId = 12345L,
            isEncrypted = false,
            mediaCounts = countsNormal,
            hasStories = true
        )
        assertTrue(tabsNormal.any { it.type == SharedMediaTabType.STORIES })
        assertTrue(tabsNormal.any { it.type == SharedMediaTabType.PHOTO_VIDEO && it.count == 15 })
        assertTrue(tabsNormal.any { it.type == SharedMediaTabType.FILES && it.count == 3 })
        assertTrue(tabsNormal.any { it.type == SharedMediaTabType.LINKS && it.count == 7 })
        assertTrue(tabsNormal.any { it.type == SharedMediaTabType.POLLS && it.count == 1 })

        // 2. Секретный чат: ссылки, опросы, истории запрещены
        val tabsSecret = resolveAvailableTabsUseCase(
            dialogId = 12345L,
            isEncrypted = true,
            mediaCounts = countsNormal,
            hasStories = true
        )
        assertFalse(tabsSecret.any { it.type == SharedMediaTabType.STORIES })
        assertFalse(tabsSecret.any { it.type == SharedMediaTabType.LINKS })
        assertFalse(tabsSecret.any { it.type == SharedMediaTabType.POLLS })
        assertTrue(tabsSecret.any { it.type == SharedMediaTabType.PHOTO_VIDEO })
        assertTrue(tabsSecret.any { it.type == SharedMediaTabType.FILES })
        assertTrue(tabsSecret.any { it.type == SharedMediaTabType.AUDIO })

        // 3. Супергруппа с вкладкой участников и подарками
        val tabsGroup = resolveAvailableTabsUseCase(
            dialogId = -100123456L,
            isEncrypted = false,
            isGroupOrChannel = true,
            hasMembersTab = true,
            hasGifts = true,
            hasRecommendations = true
        )
        assertTrue(tabsGroup.any { it.type == SharedMediaTabType.GIFTS })
        assertTrue(tabsGroup.any { it.type == SharedMediaTabType.GROUP_USERS })
        assertTrue(tabsGroup.any { it.type == SharedMediaTabType.RECOMMENDED_CHANNELS })
    }

    @Test
    fun testFilterSharedMedia() {
        val photo1 = SharedMediaItem(id = 1, messageId = 101, dialogId = 1, date = 1000, monthKey = "Jan 2026", isPhoto = true, isVideo = false)
        val photo2 = SharedMediaItem(id = 2, messageId = 102, dialogId = 1, date = 1001, monthKey = "Jan 2026", isPhoto = true, isVideo = false)
        val video1 = SharedMediaItem(id = 3, messageId = 103, dialogId = 1, date = 1002, monthKey = "Jan 2026", isPhoto = false, isVideo = true)
        val video2 = SharedMediaItem(id = 4, messageId = 104, dialogId = 1, date = 1003, monthKey = "Jan 2026", isPhoto = false, isVideo = true)
        val allItems = listOf(photo1, photo2, video1, video2)

        // ALL
        val resAll = filterSharedMediaUseCase(allItems, SharedMediaFilterType.ALL)
        assertEquals(4, resAll.size)

        // PHOTOS_ONLY
        val resPhotos = filterSharedMediaUseCase(allItems, SharedMediaFilterType.PHOTOS_ONLY)
        assertEquals(2, resPhotos.size)
        assertTrue(resPhotos.all { it.isPhoto && !it.isVideo })

        // VIDEOS_ONLY
        val resVideos = filterSharedMediaUseCase(allItems, SharedMediaFilterType.VIDEOS_ONLY)
        assertEquals(2, resVideos.size)
        assertTrue(resVideos.all { it.isVideo })
    }

    @Test
    fun testGroupMediaByMonthAndCalculateFastScrollPeriods() {
        val item1 = SharedMediaItem(id = 1, messageId = 101, dialogId = 1, date = 1767225600L, monthKey = "January 2026")
        val item2 = SharedMediaItem(id = 2, messageId = 102, dialogId = 1, date = 1767312000L, monthKey = "January 2026")
        val item3 = SharedMediaItem(id = 3, messageId = 103, dialogId = 1, date = 1764547200L, monthKey = "December 2025")
        val items = listOf(item1, item2, item3)

        val groups = groupMediaByMonthUseCase(items)
        assertEquals(2, groups.size)
        assertEquals(2, groups["January 2026"]?.size)
        assertEquals(1, groups["December 2025"]?.size)

        val periods = SharedMediaMapper.calculateFastScrollPeriods(items)
        assertEquals(2, periods.size)
        assertEquals("January 2026", periods[0].formattedDate)
        assertEquals(0, periods[0].startOffset)
        assertEquals(101, periods[0].maxId)

        assertEquals("December 2025", periods[1].formattedDate)
        assertEquals(2, periods[1].startOffset)
        assertEquals(103, periods[1].maxId)
    }

    @Test
    fun testCalculateMediaSelection() {
        val item1 = SharedMediaItem(id = 1, messageId = 101, dialogId = 1, date = 1000, monthKey = "Jan 2026")
        val item2 = SharedMediaItem(id = 2, messageId = 102, dialogId = 1, date = 1001, monthKey = "Jan 2026")
        val items = listOf(item1, item2)

        // 1. Пустой выбор
        val s0 = calculateMediaSelectionUseCase(items, emptySet())
        assertFalse(s0.isSelectionActive)
        assertEquals(0, s0.count)
        assertFalse(s0.canForward)
        assertFalse(s0.canDelete)
        assertFalse(s0.canPin)

        // 2. Выбран 1 элемент
        val s1 = calculateMediaSelectionUseCase(items, setOf(101), canPinMessages = true, canDeleteMessages = true)
        assertTrue(s1.isSelectionActive)
        assertEquals(1, s1.count)
        assertTrue(s1.canForward)
        assertTrue(s1.canDelete)
        assertTrue(s1.canPin)

        // 3. Выбрано 2 элемента (закреплять можно только 1)
        val s2 = calculateMediaSelectionUseCase(items, setOf(101, 102), canPinMessages = true, canDeleteMessages = true)
        assertTrue(s2.isSelectionActive)
        assertEquals(2, s2.count)
        assertTrue(s2.canForward)
        assertTrue(s2.canDelete)
        assertFalse(s2.canPin)
    }

    @Test
    fun testRepositoryAndViewModelMvi() = runTest {
        val vm = SharedMediaViewModel(
            observeSharedMediaStateUseCase = observeSharedMediaStateUseCase,
            selectSharedMediaTabUseCase = selectSharedMediaTabUseCase,
            setSharedMediaFilterUseCase = setSharedMediaFilterUseCase,
            toggleMediaSelectionUseCase = toggleMediaSelectionUseCase,
            clearMediaSelectionUseCase = clearMediaSelectionUseCase,
            repository = repository
        )

        advanceUntilIdle()
        val s0 = vm.uiState.value
        assertEquals(0L, s0.dialogId)
        assertEquals(SharedMediaTabType.PHOTO_VIDEO, s0.currentTab)
        assertEquals(SharedMediaFilterType.ALL, s0.filterType)
        assertTrue(s0.items.isEmpty())

        // 1. Конфигурация диалога и вкладок
        vm.onEvent(SharedMediaEvent.OnDialogConfigured(dialogId = 999L, isEncrypted = false))
        val tabs = listOf(
            org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec(
                type = SharedMediaTabType.PHOTO_VIDEO,
                title = "Media",
                count = 2
            ),
            org.telegram.messenger.feature.media.sharedmedia.domain.model.SharedMediaTabSpec(
                type = SharedMediaTabType.FILES,
                title = "Files",
                count = 5
            )
        )
        vm.onEvent(SharedMediaEvent.OnAvailableTabsUpdated(tabs))
        advanceUntilIdle()
        assertEquals(999L, vm.uiState.value.dialogId)
        assertEquals(2, vm.uiState.value.availableTabs.size)

        // 2. Загрузка элементов
        val item1 = SharedMediaItem(id = 1, messageId = 201, dialogId = 999L, date = 1000, monthKey = "Jan 2026", isPhoto = true)
        val item2 = SharedMediaItem(id = 2, messageId = 202, dialogId = 999L, date = 1001, monthKey = "Jan 2026", isVideo = true)
        vm.onEvent(SharedMediaEvent.OnItemsLoaded(listOf(item1, item2), hasMore = false))
        advanceUntilIdle()
        val s1 = vm.uiState.value
        assertEquals(2, s1.items.size)
        assertFalse(s1.hasMore)
        assertEquals(1, s1.sections.size)

        // 3. Выделение элементов
        vm.onEvent(SharedMediaEvent.OnItemSelectionToggled(messageId = 201))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSelectionActive)
        assertEquals(1, vm.uiState.value.selectedCount)
        assertTrue(vm.uiState.value.canPin)

        vm.onEvent(SharedMediaEvent.OnItemSelectionToggled(messageId = 202))
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.selectedCount)
        assertFalse(vm.uiState.value.canPin)

        // 4. Очистка выделения
        vm.onEvent(SharedMediaEvent.OnClearSelectionRequested)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSelectionActive)

        // 5. Фильтрация (только фото)
        vm.onEvent(SharedMediaEvent.OnFilterChanged(SharedMediaFilterType.PHOTOS_ONLY))
        advanceUntilIdle()
        assertEquals(SharedMediaFilterType.PHOTOS_ONLY, vm.uiState.value.filterType)
        assertEquals(1, vm.uiState.value.items.size)
        assertEquals(201, vm.uiState.value.items[0].messageId)

        // 6. Удаление выделенного
        vm.onEvent(SharedMediaEvent.OnItemSelectionToggled(messageId = 201))
        advanceUntilIdle()
        vm.onEvent(SharedMediaEvent.OnDeleteSelectedRequested)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())

        // 7. Смена вкладки
        vm.onEvent(SharedMediaEvent.OnTabSelected(SharedMediaTabType.FILES))
        advanceUntilIdle()
        assertEquals(SharedMediaTabType.FILES, vm.uiState.value.currentTab)
    }
}
