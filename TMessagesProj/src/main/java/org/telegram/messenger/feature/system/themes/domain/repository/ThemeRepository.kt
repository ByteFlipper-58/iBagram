package org.telegram.messenger.feature.system.themes.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel

/**
 * Repository interface for managing Telegram themes, appearance, auto night mode, and wallpaper.
 */
interface ThemeRepository {
    /**
     * Observes real-time changes to the appearance settings (theme, night mode, bubble radius, wallpaper).
     */
    fun observeAppearanceSettings(): Flow<AppearanceSettingsModel>

    /**
     * Synchronously returns current appearance settings snapshot.
     */
    fun getAppearanceSettings(): AppearanceSettingsModel

    /**
     * Observes list of available themes.
     */
    fun observeAvailableThemes(): Flow<List<ThemeModel>>

    /**
     * Synchronously returns current list of available themes.
     */
    fun getAvailableThemes(): List<ThemeModel>

    /**
     * Applies a theme by its key.
     */
    suspend fun applyTheme(themeKey: String, nightTheme: Boolean = false): Result<Unit>

    /**
     * Sets automatic night mode type.
     */
    suspend fun setNightModeType(type: NightModeType): Result<Unit>

    /**
     * Updates full night mode configuration.
     */
    suspend fun setNightModeSettings(settings: NightModeSettingsModel): Result<Unit>

    /**
     * Sets an accent color for a given theme.
     */
    suspend fun setThemeAccent(themeKey: String, accentId: Int): Result<Unit>

    /**
     * Updates message bubble corner radius.
     */
    suspend fun setBubbleRadius(radius: Int): Result<Unit>

    /**
     * Resets appearance settings to system/Telegram defaults.
     */
    suspend fun resetToDefault(): Result<Unit>
}
