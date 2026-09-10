package org.telegram.messenger.feature.messaging.emojipicker.domain.usecase

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerFilter
import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType

class ResolveAvailablePickerTabsUseCase {
    operator fun invoke(filter: EmojiPickerFilter): List<EmojiPickerTabType> {
        val tabs = mutableListOf<EmojiPickerTabType>()
        if (filter.allowEmoji) {
            tabs.add(EmojiPickerTabType.EMOJI)
        }
        if (filter.allowGifs) {
            tabs.add(EmojiPickerTabType.GIFS)
        }
        if (filter.allowStickers) {
            tabs.add(EmojiPickerTabType.STICKERS)
        }
        return if (tabs.isEmpty()) {
            listOf(EmojiPickerTabType.EMOJI)
        } else {
            tabs
        }
    }
}
