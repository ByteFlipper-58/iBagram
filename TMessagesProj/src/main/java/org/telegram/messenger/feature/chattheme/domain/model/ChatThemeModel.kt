package org.telegram.messenger.feature.chattheme.domain.model

data class ChatThemeModel(
    val emoticon: String? = null,
    val giftSlug: String? = null,
    val isDefault: Boolean = false,
    val isDark: Boolean = false,
    val themeId: Long = 0L,
    val wallpaperId: Long = 0L,
    val accentColor: Int? = null,
    val previewColors: List<Int> = emptyList(),
) {
    val key: String
        get() = when {
            !giftSlug.isNullOrEmpty() -> "gift_$giftSlug"
            !emoticon.isNullOrEmpty() -> "emoticon_$emoticon"
            else -> "default"
        }

    val isGiftTheme: Boolean
        get() = !giftSlug.isNullOrEmpty()
}
