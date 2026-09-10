package org.telegram.messenger.feature.emojipicker.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerPackItem
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository
import org.telegram.messenger.feature.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase

class LegacyEmojiPickerRepository(
    private val resolveTabsUseCase: ResolveAvailablePickerTabsUseCase = ResolveAvailablePickerTabsUseCase()
) : EmojiPickerRepository {

    private val _state = MutableStateFlow(EmojiPickerState())
    private val lock = Any()

    override fun observeState(): StateFlow<EmojiPickerState> = _state.asStateFlow()

    override fun getState(): EmojiPickerState = _state.value

    override fun configureFilter(filter: EmojiPickerFilter) {
        synchronized(lock) {
            val available = resolveTabsUseCase(filter)
            _state.update { current ->
                val newTab = if (available.contains(current.currentTab)) {
                    current.currentTab
                } else {
                    available.firstOrNull() ?: EmojiPickerTabType.EMOJI
                }
                current.copy(
                    filter = filter,
                    availableTabs = available,
                    currentTab = newTab
                )
            }
        }
    }

    override fun selectTab(tab: EmojiPickerTabType) {
        synchronized(lock) {
            _state.update { current ->
                if (current.availableTabs.contains(tab)) {
                    current.copy(currentTab = tab)
                } else {
                    current
                }
            }
        }
    }

    override fun setSearchQuery(query: String) {
        synchronized(lock) {
            _state.update { it.copy(searchQuery = query) }
        }
    }

    override fun setSearchActive(isActive: Boolean) {
        synchronized(lock) {
            _state.update { it.copy(isSearchActive = isActive) }
        }
    }

    override fun setRecentEmojis(emojis: List<EmojiItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentEmojis = emojis) }
        }
    }

    override fun setStickerPacks(packs: List<StickerPackItem>) {
        synchronized(lock) {
            _state.update { it.copy(installedStickerPacks = packs) }
        }
    }

    override fun setRecentStickers(stickers: List<StickerItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentStickers = stickers) }
        }
    }

    override fun setFavoriteStickers(stickers: List<StickerItem>) {
        synchronized(lock) {
            _state.update { it.copy(favoriteStickers = stickers) }
        }
    }

    override fun toggleStickerFavorite(stickerId: Long) {
        synchronized(lock) {
            _state.update { current ->
                val exists = current.favoriteStickers.any { it.id == stickerId }
                val updatedFavorites = if (exists) {
                    current.favoriteStickers.filterNot { it.id == stickerId }
                } else {
                    val matchingRecent = current.recentStickers.find { it.id == stickerId }
                    val itemToAdd = matchingRecent?.copy(isFavorite = true) ?: StickerItem(id = stickerId, isFavorite = true)
                    current.favoriteStickers + itemToAdd
                }
                current.copy(favoriteStickers = updatedFavorites)
            }
        }
    }

    override fun setRecentGifs(gifs: List<GifItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentGifs = gifs) }
        }
    }

    override fun setTrendingGifs(gifs: List<GifItem>) {
        synchronized(lock) {
            _state.update { it.copy(trendingGifs = gifs) }
        }
    }

    override fun clearRecent(tab: EmojiPickerTabType) {
        synchronized(lock) {
            _state.update { current ->
                when (tab) {
                    EmojiPickerTabType.EMOJI -> current.copy(recentEmojis = emptyList())
                    EmojiPickerTabType.STICKERS -> current.copy(recentStickers = emptyList())
                    EmojiPickerTabType.GIFS -> current.copy(recentGifs = emptyList())
                }
            }
        }
    }

    override fun setLoading(isLoading: Boolean) {
        synchronized(lock) {
            _state.update { it.copy(isLoading = isLoading) }
        }
    }

    override fun clear() {
        synchronized(lock) {
            _state.value = EmojiPickerState()
        }
    }
}
