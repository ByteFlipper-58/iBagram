package org.telegram.messenger.feature.themes.data.mapper

import org.telegram.messenger.feature.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.themes.domain.model.NightModeType
import org.telegram.messenger.feature.themes.domain.model.ThemeAccentModel
import org.telegram.messenger.feature.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.themes.domain.model.WallpaperModel
import org.telegram.ui.ActionBar.Theme

/**
 * Pure mapping functions between legacy Telegram theme structures and clean domain models.
 */
object ThemeMapper {

    fun mapThemeAccent(accent: Theme.ThemeAccent?): ThemeAccentModel? {
        if (accent == null) return null
        return ThemeAccentModel(
            id = accent.id,
            accentColor = accent.accentColor,
            accentColor2 = accent.accentColor2,
            myMessagesAccentColor = accent.myMessagesAccentColor,
            isDefault = accent.isDefault
        )
    }

    fun mapTheme(themeInfo: Theme.ThemeInfo?): ThemeModel {
        if (themeInfo == null) {
            return ThemeModel(
                key = "default",
                name = "Default",
                isDark = false,
                isDefault = true
            )
        }

        val accents = themeInfo.themeAccents?.mapNotNull { mapThemeAccent(it) } ?: emptyList()

        val themeName = try {
            themeInfo.getName() ?: themeInfo.name ?: "Unknown"
        } catch (_: Throwable) {
            themeInfo.name ?: "Unknown"
        }

        val isDarkTheme = try {
            themeInfo.isDark()
        } catch (_: Throwable) {
            "Dark Blue".equals(themeInfo.name, ignoreCase = true) || "Night".equals(themeInfo.name, ignoreCase = true)
        }

        val themeKey = try {
            themeInfo.key ?: themeInfo.name ?: "unknown"
        } catch (_: Throwable) {
            themeInfo.name ?: "unknown"
        }

        return ThemeModel(
            key = themeKey,
            name = themeName,
            pathToFile = themeInfo.pathToFile,
            assetName = themeInfo.assetName,
            slug = themeInfo.slug,
            isDark = isDarkTheme,
            isDefault = "Blue".equals(themeInfo.name, ignoreCase = true) || "Default".equals(themeInfo.name, ignoreCase = true),
            accents = accents,
            currentAccentId = themeInfo.currentAccentId,
            previewColor = themeInfo.accentBaseColor,
            isLoaded = themeInfo.themeLoaded
        )
    }

    fun mapNightModeSettings(): NightModeSettingsModel {
        return try {
            NightModeSettingsModel(
                type = NightModeType.fromValue(Theme.selectedAutoNightType),
                scheduleByLocation = Theme.autoNightScheduleByLocation,
                brightnessThreshold = Theme.autoNightBrighnessThreshold,
                dayStartTime = Theme.autoNightDayStartTime,
                dayEndTime = Theme.autoNightDayEndTime,
                cityName = Theme.autoNightCityName ?: ""
            )
        } catch (_: Throwable) {
            NightModeSettingsModel()
        }
    }

    fun mapWallpaper(): WallpaperModel {
        val slug = try {
            Theme.getSelectedBackgroundSlug() ?: ""
        } catch (_: Throwable) {
            ""
        }
        val isDefault = try {
            Theme.DEFAULT_BACKGROUND_SLUG == slug
        } catch (_: Throwable) {
            true
        }
        val isFromTheme = try {
            Theme.hasWallpaperFromTheme()
        } catch (_: Throwable) {
            false
        }
        return WallpaperModel(
            slug = slug,
            isDefault = isDefault,
            isFromTheme = isFromTheme
        )
    }

    fun mapAppearanceSettings(bubbleRadius: Int): AppearanceSettingsModel {
        val currentThemeInfo = Theme.getCurrentTheme() ?: Theme.getActiveTheme()
        val currentNightThemeInfo = Theme.getCurrentNightTheme()

        return AppearanceSettingsModel(
            currentTheme = mapTheme(currentThemeInfo),
            currentNightTheme = currentNightThemeInfo?.let { mapTheme(it) },
            isNightModeActive = Theme.isCurrentThemeNight(),
            nightModeSettings = mapNightModeSettings(),
            bubbleRadius = bubbleRadius,
            wallpaper = mapWallpaper()
        )
    }
}
