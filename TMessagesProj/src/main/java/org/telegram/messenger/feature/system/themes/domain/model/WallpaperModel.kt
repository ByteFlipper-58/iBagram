package org.telegram.messenger.feature.system.themes.domain.model

/**
 * Domain representation of chat background wallpaper.
 */
data class WallpaperModel(
    val slug: String = "",
    val isDefault: Boolean = true,
    val isFromTheme: Boolean = false
)
