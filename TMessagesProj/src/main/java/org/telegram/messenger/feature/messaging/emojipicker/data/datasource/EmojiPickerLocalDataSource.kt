package org.telegram.messenger.feature.messaging.emojipicker.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerPackItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase

class EmojiPickerLocalDataSource(
    private val currentAccount: Int = 0,
    private val resolveTabsUseCase: ResolveAvailablePickerTabsUseCase = ResolveAvailablePickerTabsUseCase()
) {
    private val _state = MutableStateFlow(EmojiPickerState())
    val state: StateFlow<EmojiPickerState> = _state.asStateFlow()
    private val lock = Any()

    fun getState(): EmojiPickerState = _state.value

    fun configureFilter(filter: EmojiPickerFilter) {
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

    fun selectTab(tab: EmojiPickerTabType) {
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

    fun setSearchQuery(query: String) {
        synchronized(lock) {
            _state.update { it.copy(searchQuery = query) }
        }
    }

    fun setSearchActive(isActive: Boolean) {
        synchronized(lock) {
            _state.update { it.copy(isSearchActive = isActive) }
        }
    }

    fun setRecentEmojis(emojis: List<EmojiItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentEmojis = emojis) }
        }
    }

    fun setStickerPacks(packs: List<StickerPackItem>) {
        synchronized(lock) {
            _state.update { it.copy(installedStickerPacks = packs) }
        }
    }

    fun setRecentStickers(stickers: List<StickerItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentStickers = stickers) }
        }
    }

    fun setFavoriteStickers(stickers: List<StickerItem>) {
        synchronized(lock) {
            _state.update { it.copy(favoriteStickers = stickers) }
        }
    }

    fun toggleStickerFavorite(stickerId: Long) {
        synchronized(lock) {
            _state.update { current ->
                val exists = current.favoriteStickers.any { it.id == stickerId }
                val updatedFavorites = if (exists) {
                    current.favoriteStickers.filterNot { it.id == stickerId }
                } else {
                    val foundInRecent = current.recentStickers.firstOrNull { it.id == stickerId }
                    if (foundInRecent != null) {
                        current.favoriteStickers + foundInRecent.copy(isFavorite = true)
                    } else {
                        current.favoriteStickers
                    }
                }
                current.copy(favoriteStickers = updatedFavorites)
            }
        }
    }

    fun setRecentGifs(gifs: List<GifItem>) {
        synchronized(lock) {
            _state.update { it.copy(recentGifs = gifs) }
        }
    }

    fun setTrendingGifs(gifs: List<GifItem>) {
        synchronized(lock) {
            _state.update { it.copy(trendingGifs = gifs) }
        }
    }

    fun clearRecent(tab: EmojiPickerTabType) {
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

    fun setLoading(isLoading: Boolean) {
        synchronized(lock) {
            _state.update { it.copy(isLoading = isLoading) }
        }
    }

    fun clear() {
        synchronized(lock) {
            _state.value = EmojiPickerState()
        }
    }
}
