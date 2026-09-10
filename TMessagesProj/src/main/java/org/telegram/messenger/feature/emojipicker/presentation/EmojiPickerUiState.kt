package org.telegram.messenger.feature.emojipicker.presentation

import org.telegram.messenger.feature.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerPackItem

data class EmojiPickerUiState(
    val currentTab: EmojiPickerTabType = EmojiPickerTabType.EMOJI,
    val availableTabs: List<EmojiPickerTabType> = listOf(
        EmojiPickerTabType.EMOJI,
        EmojiPickerTabType.GIFS,
        EmojiPickerTabType.STICKERS
    ),
    val filter: EmojiPickerFilter = EmojiPickerFilter(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val recentEmojis: List<EmojiItem> = emptyList(),
    val installedStickerPacks: List<StickerPackItem> = emptyList(),
    val recentStickers: List<StickerItem> = emptyList(),
    val favoriteStickers: List<StickerItem> = emptyList(),
    val recentGifs: List<GifItem> = emptyList(),
    val trendingGifs: List<GifItem> = emptyList(),
    val selectedEmoji: EmojiItem? = null,
    val selectedSticker: StickerItem? = null,
    val selectedGif: GifItem? = null
) {
    val hasTabs: Boolean get() = availableTabs.size > 1
    val hasSearchQuery: Boolean get() = searchQuery.isNotEmpty()
    val canClearRecent: Boolean get() = when (currentTab) {
        EmojiPickerTabType.EMOJI -> recentEmojis.isNotEmpty()
        EmojiPickerTabType.STICKERS -> recentStickers.isNotEmpty()
        EmojiPickerTabType.GIFS -> recentGifs.isNotEmpty()
    }
}
