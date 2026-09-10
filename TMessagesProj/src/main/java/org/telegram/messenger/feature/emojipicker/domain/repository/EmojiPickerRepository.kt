package org.telegram.messenger.feature.emojipicker.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerState
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerPackItem

interface EmojiPickerRepository {
    fun observeState(): StateFlow<EmojiPickerState>
    fun getState(): EmojiPickerState
    fun configureFilter(filter: EmojiPickerFilter)
    fun selectTab(tab: EmojiPickerTabType)
    fun setSearchQuery(query: String)
    fun setSearchActive(isActive: Boolean)
    fun setRecentEmojis(emojis: List<EmojiItem>)
    fun setStickerPacks(packs: List<StickerPackItem>)
    fun setRecentStickers(stickers: List<StickerItem>)
    fun setFavoriteStickers(stickers: List<StickerItem>)
    fun toggleStickerFavorite(stickerId: Long)
    fun setRecentGifs(gifs: List<GifItem>)
    fun setTrendingGifs(gifs: List<GifItem>)
    fun clearRecent(tab: EmojiPickerTabType)
    fun setLoading(isLoading: Boolean)
    fun clear()
}
