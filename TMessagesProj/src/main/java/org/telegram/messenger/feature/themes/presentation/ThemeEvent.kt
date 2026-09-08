package org.telegram.messenger.feature.themes.presentation

import org.telegram.messenger.feature.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.themes.domain.model.NightModeType

/**
 * MVI Events for Theme management.
 */
sealed class ThemeEvent {
    data object Load : ThemeEvent()
    data class SelectTheme(val themeKey: String, val nightTheme: Boolean = false) : ThemeEvent()
    data class SelectThemeAccent(val themeKey: String, val accentId: Int) : ThemeEvent()
    data class SetNightMode(val type: NightModeType) : ThemeEvent()
    data class UpdateNightModeSettings(val settings: NightModeSettingsModel) : ThemeEvent()
    data class SetBubbleRadius(val radius: Int) : ThemeEvent()
    data object ResetToDefaults : ThemeEvent()
    data object ClearMessage : ThemeEvent()
}
