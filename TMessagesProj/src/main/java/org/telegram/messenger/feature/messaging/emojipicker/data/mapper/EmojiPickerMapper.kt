package org.telegram.messenger.feature.messaging.emojipicker.data.mapper

import org.telegram.messenger.feature.messaging.emojipicker.domain.model.EmojiPickerTabType

object EmojiPickerMapper {
    const val TAB_EMOJI_INT = 0
    const val TAB_GIFS_INT = 1
    const val TAB_STICKERS_INT = 2

    fun mapIntToTabType(type: Int): EmojiPickerTabType {
        return when (type) {
            TAB_GIFS_INT -> EmojiPickerTabType.GIFS
            TAB_STICKERS_INT -> EmojiPickerTabType.STICKERS
            else -> EmojiPickerTabType.EMOJI
        }
    }

    fun mapTabTypeToInt(tab: EmojiPickerTabType): Int {
        return when (tab) {
            EmojiPickerTabType.EMOJI -> TAB_EMOJI_INT
            EmojiPickerTabType.GIFS -> TAB_GIFS_INT
            EmojiPickerTabType.STICKERS -> TAB_STICKERS_INT
        }
    }

    fun normalizeQuery(rawQuery: String?): String {
        return rawQuery?.trim().orEmpty()
    }
}
