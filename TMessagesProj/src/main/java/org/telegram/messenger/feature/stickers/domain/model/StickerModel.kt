package org.telegram.messenger.feature.stickers.domain.model

/**
 * Pure Kotlin immutable domain model representing an individual sticker or custom emoji.
 */
data class StickerModel(
    val id: Long,
    val accessHash: Long = 0L,
    val setId: Long = 0L,
    val mimeType: String = "",
    val emoji: String = "",
    val type: StickerType = StickerType.IMAGE,
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L
)
