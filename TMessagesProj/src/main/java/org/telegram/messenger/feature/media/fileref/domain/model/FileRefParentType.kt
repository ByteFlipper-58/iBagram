package org.telegram.messenger.feature.media.fileref.domain.model

/**
 * Categorization of parent objects capable of renewing expired MTProto file references.
 */
enum class FileRefParentType {
    MESSAGE,
    STORY,
    USER,
    CHAT,
    WALLPAPER,
    THEME,
    STICKER_SET,
    SAVED_GIF,
    BOT_INFO,
    ATTACH_MENU_BOT,
    PREMIUM_PROMO,
    AVAILABLE_REACTION,
    CUSTOM
}
