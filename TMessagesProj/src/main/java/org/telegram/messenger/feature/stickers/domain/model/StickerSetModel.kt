package org.telegram.messenger.feature.stickers.domain.model

/**
 * Pure Kotlin immutable domain model representing a sticker set / pack.
 */
data class StickerSetModel(
    val id: Long,
    val accessHash: Long = 0L,
    val title: String,
    val shortName: String,
    val count: Int = 0,
    val isInstalled: Boolean = false,
    val isArchived: Boolean = false,
    val isOfficial: Boolean = false,
    val isAnimated: Boolean = false,
    val isVideo: Boolean = false,
    val isEmoji: Boolean = false,
    val stickers: List<StickerModel> = emptyList()
)
