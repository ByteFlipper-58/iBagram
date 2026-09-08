package org.telegram.messenger.feature.themes.domain.model

/**
 * Settings configuration for automatic night mode switching.
 */
data class NightModeSettingsModel(
    val type: NightModeType = NightModeType.NONE,
    val scheduleByLocation: Boolean = false,
    val brightnessThreshold: Float = 0.25f,
    val dayStartTime: Int = 22 * 60,
    val dayEndTime: Int = 8 * 60,
    val cityName: String = ""
)
