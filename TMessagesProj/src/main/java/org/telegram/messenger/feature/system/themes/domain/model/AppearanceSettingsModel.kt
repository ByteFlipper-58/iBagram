package org.telegram.messenger.feature.system.themes.domain.model

/**
 * Aggregated domain model representing overall appearance state in Telegram.
 */
data class AppearanceSettingsModel(
    val currentTheme: ThemeModel,
    val currentNightTheme: ThemeModel? = null,
    val isNightModeActive: Boolean = false,
    val nightModeSettings: NightModeSettingsModel = NightModeSettingsModel(),
    val bubbleRadius: Int = 17,
    val wallpaper: WallpaperModel = WallpaperModel()
)
