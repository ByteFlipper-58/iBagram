package org.telegram.messenger.feature.system.themes.data.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.data.mapper.ThemeMapper
import org.telegram.messenger.feature.system.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel
import org.telegram.ui.ActionBar.Theme

/**
 * Local data source managing active themes, appearance configuration,
 * bubble radius, and auto night mode states.
 */
class ThemesLocalDataSource(
    private val currentAccount: Int = 0,
    var testMode: Boolean = false
) {

    private val defaultTheme = ThemeModel(key = "Blue", name = "Day", isDark = false, isDefault = true)

    private val appearanceSettingsFlow = MutableStateFlow(
        AppearanceSettingsModel(currentTheme = defaultTheme)
    )
    private val availableThemesFlow = MutableStateFlow<List<ThemeModel>>(
        listOf(
            defaultTheme,
            ThemeModel(key = "Dark Blue", name = "Night", isDark = true, isDefault = false)
        )
    )

    fun observeAppearanceSettings(): Flow<AppearanceSettingsModel> = appearanceSettingsFlow.asStateFlow()

    fun getAppearanceSettings(): AppearanceSettingsModel {
        if (testMode) {
            return appearanceSettingsFlow.value
        }
        return try {
            ThemeMapper.mapAppearanceSettings(SharedConfig.bubbleRadius)
        } catch (_: Throwable) {
            appearanceSettingsFlow.value
        }
    }

    fun observeAvailableThemes(): Flow<List<ThemeModel>> = availableThemesFlow.asStateFlow()

    fun getAvailableThemes(): List<ThemeModel> {
        if (testMode) {
            return availableThemesFlow.value
        }
        return try {
            val themesList = Theme.themes ?: emptyList()
            if (themesList.isEmpty()) availableThemesFlow.value else themesList.map { ThemeMapper.mapTheme(it) }
        } catch (_: Throwable) {
            availableThemesFlow.value
        }
    }

    suspend fun applyTheme(themeKey: String, nightTheme: Boolean = false): Result<Unit> {
        if (testMode) {
            val current = appearanceSettingsFlow.value
            appearanceSettingsFlow.value = current.copy(
                currentTheme = ThemeModel(key = themeKey, name = themeKey, isDark = nightTheme)
            )
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                val themesList = Theme.themes ?: return@withContext Result.Failure(AppError.NotFound("Themes list is empty"))
                val target = themesList.find { it.key == themeKey || it.name == themeKey }
                    ?: return@withContext Result.Failure(AppError.NotFound("Theme with key '$themeKey' not found"))

                Theme.applyTheme(target, nightTheme)
                appearanceSettingsFlow.value = getAppearanceSettings()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to apply theme: ${e.message}", e))
            }
        }
    }

    suspend fun setNightModeType(type: NightModeType): Result<Unit> {
        if (testMode) {
            val current = appearanceSettingsFlow.value
            appearanceSettingsFlow.value = current.copy(
                nightModeSettings = current.nightModeSettings.copy(type = type)
            )
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                Theme.selectedAutoNightType = type.value
                Theme.saveAutoNightThemeConfig()
                Theme.checkAutoNightThemeConditions()
                runCatching {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme)
                }
                appearanceSettingsFlow.value = getAppearanceSettings()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to set night mode type: ${e.message}", e))
            }
        }
    }

    suspend fun setNightModeSettings(settings: NightModeSettingsModel): Result<Unit> {
        if (testMode) {
            val current = appearanceSettingsFlow.value
            appearanceSettingsFlow.value = current.copy(nightModeSettings = settings)
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                Theme.selectedAutoNightType = settings.type.value
                Theme.autoNightScheduleByLocation = settings.scheduleByLocation
                Theme.autoNightBrighnessThreshold = settings.brightnessThreshold
                Theme.autoNightDayStartTime = settings.dayStartTime
                Theme.autoNightDayEndTime = settings.dayEndTime
                Theme.autoNightCityName = settings.cityName
                Theme.saveAutoNightThemeConfig()
                Theme.checkAutoNightThemeConditions()
                runCatching {
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme)
                }
                appearanceSettingsFlow.value = getAppearanceSettings()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to set night mode settings: ${e.message}", e))
            }
        }
    }

    suspend fun setThemeAccent(themeKey: String, accentId: Int): Result<Unit> {
        if (testMode) {
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                val themesList = Theme.themes ?: return@withContext Result.Failure(AppError.NotFound("Themes list is empty"))
                val target = themesList.find { it.key == themeKey || it.name == themeKey }
                    ?: return@withContext Result.Failure(AppError.NotFound("Theme with key '$themeKey' not found"))

                val accent = target.themeAccentsMap?.get(accentId)
                if (accent != null) {
                    target.setCurrentAccentId(accentId)
                    Theme.saveThemeAccents(target, true, false, true, false)
                    appearanceSettingsFlow.value = getAppearanceSettings()
                    Result.Success(Unit)
                } else {
                    Result.Failure(AppError.NotFound("Accent with ID '$accentId' not found"))
                }
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to set theme accent: ${e.message}", e))
            }
        }
    }

    suspend fun setBubbleRadius(radius: Int): Result<Unit> {
        if (testMode) {
            val current = appearanceSettingsFlow.value
            appearanceSettingsFlow.value = current.copy(bubbleRadius = radius)
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                SharedConfig.bubbleRadius = radius
                appearanceSettingsFlow.value = getAppearanceSettings()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to set bubble radius: ${e.message}", e))
            }
        }
    }

    suspend fun resetToDefault(): Result<Unit> {
        if (testMode) {
            appearanceSettingsFlow.value = AppearanceSettingsModel(currentTheme = defaultTheme)
            return Result.Success(Unit)
        }

        return withContext(Dispatchers.IO) {
            try {
                SharedConfig.bubbleRadius = 17
                Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE
                Theme.saveAutoNightThemeConfig()
                appearanceSettingsFlow.value = getAppearanceSettings()
                Result.Success(Unit)
            } catch (e: Throwable) {
                Result.Failure(AppError.Generic("Failed to reset theme settings: ${e.message}", e))
            }
        }
    }

    fun updateAppearanceSettingsState(settings: AppearanceSettingsModel) {
        appearanceSettingsFlow.value = settings
    }

    fun updateAvailableThemesState(themes: List<ThemeModel>) {
        availableThemesFlow.value = themes
    }
}
