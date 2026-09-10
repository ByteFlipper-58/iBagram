package org.telegram.messenger.feature.messaging.emojipicker.domain.model

enum class EmojiPickerTabType {
    EMOJI,
    GIFS,
    STICKERS
}

enum class EmojiCategoryType {
    RECENT,
    FAVORITES,
    SMILEYS,
    PEOPLE,
    ANIMALS,
    FOOD,
    TRAVEL,
    ACTIVITIES,
    OBJECTS,
    SYMBOLS,
    CUSTOM_PACK
}

data class EmojiItem(
    val id: String,
    val unicode: String? = null,
    val documentId: Long? = null,
    val packId: Long? = null,
    val emoticon: String? = null,
    val isCustom: Boolean = false,
    val isPremium: Boolean = false,
    val isRecent: Boolean = false,
    val isFavorite: Boolean = false
)

data class StickerItem(
    val id: Long,
    val packId: Long = 0L,
    val packTitle: String? = null,
    val emoticon: String? = null,
    val isAnimated: Boolean = false,
    val isVideo: Boolean = false,
    val isPremium: Boolean = false,
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false
)

data class GifItem(
    val id: String,
    val query: String? = null,
    val url: String? = null,
    val thumbUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val duration: Int = 0,
    val isRecent: Boolean = false,
    val isTrending: Boolean = false
)

data class StickerPackItem(
    val id: Long,
    val accessHash: Long = 0L,
    val title: String,
    val shortName: String,
    val count: Int = 0,
    val isInstalled: Boolean = false,
    val isOfficial: Boolean = false,
    val isAnimated: Boolean = false,
    val isVideo: Boolean = false,
    val isMasks: Boolean = false,
    val isFeatured: Boolean = false
)

data class EmojiPickerFilter(
    val allowEmoji: Boolean = true,
    val allowStickers: Boolean = true,
    val allowGifs: Boolean = true,
    val allowCustomEmoji: Boolean = true,
    val allowSearch: Boolean = true
)

data class EmojiPickerState(
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
    val trendingGifs: List<GifItem> = emptyList()
)
