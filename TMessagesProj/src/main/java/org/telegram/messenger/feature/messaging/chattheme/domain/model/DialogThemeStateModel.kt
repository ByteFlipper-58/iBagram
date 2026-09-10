package org.telegram.messenger.feature.messaging.chattheme.domain.model

data class DialogThemeStateModel(
    val dialogId: Long,
    val currentTheme: ChatThemeModel? = null,
    val wallpaperSlug: String? = null,
    val wallpaperId: Long? = null,
    val hasCustomTheme: Boolean = currentTheme != null && !currentTheme.isDefault,
    val hasCustomWallpaper: Boolean = wallpaperId != null && wallpaperId != 0L,
)
