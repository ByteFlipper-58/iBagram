package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerItem
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.StickerPackItem

class FilterStickersUseCase {
    fun filterPacks(packs: List<StickerPackItem>, query: String): List<StickerPackItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return packs

        val lowerQuery = trimmed.lowercase()
        return packs.filter { pack ->
            pack.title.lowercase().contains(lowerQuery) ||
                pack.shortName.lowercase().contains(lowerQuery)
        }
    }

    fun filterStickers(stickers: List<StickerItem>, query: String): List<StickerItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return stickers

        val lowerQuery = trimmed.lowercase()
        return stickers.filter { sticker ->
            sticker.emoticon?.lowercase()?.contains(lowerQuery) == true ||
                sticker.packTitle?.lowercase()?.contains(lowerQuery) == true
        }
    }
}
