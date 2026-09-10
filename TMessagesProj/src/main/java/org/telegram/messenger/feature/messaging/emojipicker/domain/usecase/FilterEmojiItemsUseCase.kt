package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiItem

class FilterEmojiItemsUseCase {
    operator fun invoke(emojis: List<EmojiItem>, query: String): List<EmojiItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emojis

        val lowerQuery = trimmed.lowercase()
        return emojis.filter { item ->
            item.emoticon?.lowercase()?.contains(lowerQuery) == true ||
                item.unicode?.lowercase()?.contains(lowerQuery) == true ||
                item.id.lowercase().contains(lowerQuery)
        }
    }
}
