package org.telegram.messenger.feature.emojipicker.presentation

import org.telegram.messenger.feature.emojipicker.domain.model.EmojiItem
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.emojipicker.domain.model.EmojiPickerTabType
import org.telegram.messenger.feature.emojipicker.domain.model.GifItem
import org.telegram.messenger.feature.emojipicker.domain.model.StickerItem

sealed interface EmojiPickerEvent {
    data class OnFilterConfigured(val filter: EmojiPickerFilter) : EmojiPickerEvent
    data class OnTabSelected(val tab: EmojiPickerTabType) : EmojiPickerEvent
    data class OnSearchQueryChanged(val query: String) : EmojiPickerEvent
    data class OnSearchActiveChanged(val isActive: Boolean) : EmojiPickerEvent
    data class OnEmojiSelected(val emoji: EmojiItem) : EmojiPickerEvent
    data class OnStickerSelected(val sticker: StickerItem) : EmojiPickerEvent
    data class OnGifSelected(val gif: GifItem) : EmojiPickerEvent
    data class OnStickerFavoriteToggled(val stickerId: Long) : EmojiPickerEvent
    data class OnClearRecentRequested(val tab: EmojiPickerTabType) : EmojiPickerEvent
    data object OnClearRequested : EmojiPickerEvent
}
