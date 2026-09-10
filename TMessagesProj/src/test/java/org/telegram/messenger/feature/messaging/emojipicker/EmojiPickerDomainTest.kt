package org.telegram.messenger.feature.messaging.emojipicker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.telegram.messenger.feature.messaging.emojipicker.data.mapper.EmojiPickerMapper
import org.telegram.messenger.feature.messaging.emojipicker.data.repository.LegacyEmojiPickerRepository
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerPackItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ClearRecentPickerItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterEmojiItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterGifsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterStickersUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.GetEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ObserveEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.SelectPickerTabUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ToggleStickerFavoriteUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.UpdatePickerSearchQueryUseCase
import org.telegram.messenger.feature.messaging.emojipicker.presentation.EmojiPickerEvent
import org.telegram.messenger.feature.messaging.emojipicker.presentation.EmojiPickerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class EmojiPickerDomainTest {

    @Test
    fun testResolveAvailableTabs() {
        val resolveTabs = ResolveAvailablePickerTabsUseCase()

        // 1. All enabled
        val allEnabled = resolveTabs(EmojiPickerFilter(allowEmoji = true, allowGifs = true, allowStickers = true))
        assertEquals(listOf(EmojiPickerTabType.EMOJI, EmojiPickerTabType.GIFS, EmojiPickerTabType.STICKERS), allEnabled)

        // 2. Only stickers disabled
        val noStickers = resolveTabs(EmojiPickerFilter(allowEmoji = true, allowGifs = true, allowStickers = false))
        assertEquals(listOf(EmojiPickerTabType.EMOJI, EmojiPickerTabType.GIFS), noStickers)

        // 3. Only GIFs disabled
        val noGifs = resolveTabs(EmojiPickerFilter(allowEmoji = true, allowGifs = false, allowStickers = true))
        assertEquals(listOf(EmojiPickerTabType.EMOJI, EmojiPickerTabType.STICKERS), noGifs)

        // 4. Only emoji enabled
        val onlyEmoji = resolveTabs(EmojiPickerFilter(allowEmoji = true, allowGifs = false, allowStickers = false))
        assertEquals(listOf(EmojiPickerTabType.EMOJI), onlyEmoji)

        // 5. None enabled fallback to EMOJI
        val noneEnabled = resolveTabs(EmojiPickerFilter(allowEmoji = false, allowGifs = false, allowStickers = false))
        assertEquals(listOf(EmojiPickerTabType.EMOJI), noneEnabled)
    }

    @Test
    fun testFilterItemsAndMappers() {
        val filterEmojis = FilterEmojiItemsUseCase()
        val filterStickers = FilterStickersUseCase()
        val filterGifs = FilterGifsUseCase()

        // Test Emoji Filtering
        val emojis = listOf(
            EmojiItem(id = "1", emoticon = "grinning", unicode = "😀"),
            EmojiItem(id = "2", emoticon = "fire", unicode = "🔥"),
            EmojiItem(id = "3", emoticon = "heart", unicode = "❤️")
        )
        val filteredEmojis = filterEmojis(emojis, "fir")
        assertEquals(1, filteredEmojis.size)
        assertEquals("fire", filteredEmojis.first().emoticon)

        val emptyQueryEmojis = filterEmojis(emojis, "   ")
        assertEquals(3, emptyQueryEmojis.size)

        // Test Sticker Filtering
        val packs = listOf(
            StickerPackItem(id = 100L, title = "Animals Fun", shortName = "animals_fun", count = 10),
            StickerPackItem(id = 101L, title = "Space Cats", shortName = "space_cats", count = 20)
        )
        val filteredPacks = filterStickers.filterPacks(packs, "cats")
        assertEquals(1, filteredPacks.size)
        assertEquals(101L, filteredPacks.first().id)

        val stickers = listOf(
            StickerItem(id = 501L, packTitle = "Space Cats", emoticon = "🐱"),
            StickerItem(id = 502L, packTitle = "Animals Fun", emoticon = "🐶")
        )
        val filteredStickers = filterStickers.filterStickers(stickers, "🐶")
        assertEquals(1, filteredStickers.size)
        assertEquals(502L, filteredStickers.first().id)

        // Test GIF Filtering
        val gifs = listOf(
            GifItem(id = "g1", query = "happy cat"),
            GifItem(id = "g2", query = "dancing dog")
        )
        val filteredGifs = filterGifs(gifs, "cat")
        assertEquals(1, filteredGifs.size)
        assertEquals("g1", filteredGifs.first().id)

        // Test Mapper
        assertEquals(EmojiPickerTabType.EMOJI, EmojiPickerMapper.mapIntToTabType(EmojiPickerMapper.TAB_EMOJI_INT))
        assertEquals(EmojiPickerTabType.GIFS, EmojiPickerMapper.mapIntToTabType(EmojiPickerMapper.TAB_GIFS_INT))
        assertEquals(EmojiPickerTabType.STICKERS, EmojiPickerMapper.mapIntToTabType(EmojiPickerMapper.TAB_STICKERS_INT))

        assertEquals(EmojiPickerMapper.TAB_EMOJI_INT, EmojiPickerMapper.mapTabTypeToInt(EmojiPickerTabType.EMOJI))
        assertEquals(EmojiPickerMapper.TAB_GIFS_INT, EmojiPickerMapper.mapTabTypeToInt(EmojiPickerTabType.GIFS))
        assertEquals(EmojiPickerMapper.TAB_STICKERS_INT, EmojiPickerMapper.mapTabTypeToInt(EmojiPickerTabType.STICKERS))

        assertEquals("cat", EmojiPickerMapper.normalizeQuery("  cat  "))
        assertEquals("", EmojiPickerMapper.normalizeQuery(null))
    }

    @Test
    fun testStickerFavoriteToggle() {
        val repo = LegacyEmojiPickerRepository()

        val s1 = StickerItem(id = 10L, packTitle = "Pack1", emoticon = "🔥")
        val s2 = StickerItem(id = 20L, packTitle = "Pack2", emoticon = "🎉")
        repo.setRecentStickers(listOf(s1, s2))

        // Initially no favorites
        assertTrue(repo.getState().favoriteStickers.isEmpty())

        // Toggle favorite for s1 -> should add
        repo.toggleStickerFavorite(10L)
        assertEquals(1, repo.getState().favoriteStickers.size)
        assertEquals(10L, repo.getState().favoriteStickers.first().id)
        assertTrue(repo.getState().favoriteStickers.first().isFavorite)

        // Toggle favorite for s2 -> should add
        repo.toggleStickerFavorite(20L)
        assertEquals(2, repo.getState().favoriteStickers.size)

        // Toggle favorite for s1 again -> should remove
        repo.toggleStickerFavorite(10L)
        assertEquals(1, repo.getState().favoriteStickers.size)
        assertEquals(20L, repo.getState().favoriteStickers.first().id)
    }

    @Test
    fun testClearRecentItems() {
        val repo = LegacyEmojiPickerRepository()

        repo.setRecentEmojis(listOf(EmojiItem(id = "e1"), EmojiItem(id = "e2")))
        repo.setRecentStickers(listOf(StickerItem(id = 1L), StickerItem(id = 2L)))
        repo.setRecentGifs(listOf(GifItem(id = "g1"), GifItem(id = "g2")))

        assertEquals(2, repo.getState().recentEmojis.size)
        assertEquals(2, repo.getState().recentStickers.size)
        assertEquals(2, repo.getState().recentGifs.size)

        // Clear EMOJI
        repo.clearRecent(EmojiPickerTabType.EMOJI)
        assertTrue(repo.getState().recentEmojis.isEmpty())
        assertEquals(2, repo.getState().recentStickers.size)
        assertEquals(2, repo.getState().recentGifs.size)

        // Clear STICKERS
        repo.clearRecent(EmojiPickerTabType.STICKERS)
        assertTrue(repo.getState().recentStickers.isEmpty())
        assertEquals(2, repo.getState().recentGifs.size)

        // Clear GIFS
        repo.clearRecent(EmojiPickerTabType.GIFS)
        assertTrue(repo.getState().recentGifs.isEmpty())
    }

    @Test
    fun testEmojiPickerViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val repo = LegacyEmojiPickerRepository()
        val observeState = ObserveEmojiPickerStateUseCase(repo)
        val getState = GetEmojiPickerStateUseCase(repo)
        val selectTab = SelectPickerTabUseCase(repo)
        val updateSearch = UpdatePickerSearchQueryUseCase(repo)
        val toggleFavorite = ToggleStickerFavoriteUseCase(repo)
        val clearRecent = ClearRecentPickerItemsUseCase(repo)

        val vm = EmojiPickerViewModel(
            observeStateUseCase = observeState,
            getStateUseCase = getState,
            selectTabUseCase = selectTab,
            updateSearchQueryUseCase = updateSearch,
            toggleFavoriteUseCase = toggleFavorite,
            clearRecentUseCase = clearRecent,
            repository = repo,
            scope = testScope
        )

        advanceUntilIdle()
        assertEquals(EmojiPickerTabType.EMOJI, vm.uiState.value.currentTab)
        assertTrue(vm.uiState.value.hasTabs)

        // 1. Switch tab to GIFS
        vm.onEvent(EmojiPickerEvent.OnTabSelected(EmojiPickerTabType.GIFS))
        advanceUntilIdle()
        assertEquals(EmojiPickerTabType.GIFS, vm.uiState.value.currentTab)

        // 2. Search query change
        vm.onEvent(EmojiPickerEvent.OnSearchQueryChanged("reaction"))
        advanceUntilIdle()
        assertEquals("reaction", vm.uiState.value.searchQuery)
        assertTrue(vm.uiState.value.isSearchActive)
        assertTrue(vm.uiState.value.hasSearchQuery)

        // 3. Selection events
        val emoji = EmojiItem(id = "em_1", unicode = "🌟")
        vm.onEvent(EmojiPickerEvent.OnEmojiSelected(emoji))
        advanceUntilIdle()
        assertEquals(emoji, vm.uiState.value.selectedEmoji)

        val sticker = StickerItem(id = 88L, packTitle = "CoolPack")
        vm.onEvent(EmojiPickerEvent.OnStickerSelected(sticker))
        advanceUntilIdle()
        assertEquals(sticker, vm.uiState.value.selectedSticker)

        val gif = GifItem(id = "gif_1", query = "dance")
        vm.onEvent(EmojiPickerEvent.OnGifSelected(gif))
        advanceUntilIdle()
        assertEquals(gif, vm.uiState.value.selectedGif)

        // 4. Configure filter disabling GIFS -> should adjust tab and availableTabs
        vm.onEvent(EmojiPickerEvent.OnFilterConfigured(
            EmojiPickerFilter(allowEmoji = true, allowGifs = false, allowStickers = true)
        ))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.availableTabs.contains(EmojiPickerTabType.GIFS))
        assertEquals(EmojiPickerTabType.EMOJI, vm.uiState.value.currentTab)

        // 5. Clear requested
        vm.onEvent(EmojiPickerEvent.OnClearRequested)
        advanceUntilIdle()
        assertNull(vm.uiState.value.selectedEmoji)
        assertNull(vm.uiState.value.selectedSticker)
        assertNull(vm.uiState.value.selectedGif)
        assertEquals("", vm.uiState.value.searchQuery)
        assertFalse(vm.uiState.value.isSearchActive)
    }
}
