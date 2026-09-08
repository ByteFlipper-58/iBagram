package org.telegram.messenger.feature.themes.presentation

import org.telegram.messenger.feature.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.themes.domain.model.WallpaperModel

/**
 * UI State for Themes and Appearance screen.
 */
data class ThemeUiState(
    val isLoading: Boolean = false,
    val currentTheme: ThemeModel? = null,
    val currentNightTheme: ThemeModel? = null,
    val isNightModeActive: Boolean = false,
    val availableThemes: List<ThemeModel> = emptyList(),
    val nightModeSettings: NightModeSettingsModel = NightModeSettingsModel(),
    val bubbleRadius: Int = 17,
    val wallpaper: WallpaperModel = WallpaperModel(),
    val userMessage: String? = null,
    val errorMessage: String? = null
)
