package org.telegram.messenger.feature.themes.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.themes.data.mapper.ThemeMapper
import org.telegram.messenger.feature.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.themes.domain.model.NightModeType
import org.telegram.messenger.feature.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.themes.domain.repository.ThemeRepository
import org.telegram.ui.ActionBar.Theme
import kotlinx.coroutines.flow.map

/**
 * Legacy adapter implementing ThemeRepository using Theme.java and SharedConfig.
 * All mutations and state reads are dispatched safely on Dispatchers.Main.
 */
class LegacyThemeRepository(
    private val account: Int
) : ThemeRepository {

    override fun observeAppearanceSettings(): Flow<AppearanceSettingsModel> {
        val themeChangedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didSetNewTheme)
        val themeListUpdatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.themeListUpdated)
        val themeAppliedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didApplyNewTheme)
        val themeAccentUpdatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.themeAccentListUpdated)
        val dayNightUpdatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.needSetDayNightTheme)
        val wallpaperUpdatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didSetNewWallpapper)

        return merge(
            themeChangedFlow,
            themeListUpdatedFlow,
            themeAppliedFlow,
            themeAccentUpdatedFlow,
            dayNightUpdatedFlow,
            wallpaperUpdatedFlow
        )
            .map { getAppearanceSettings() }
            .onStart { emit(getAppearanceSettings()) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Main)
    }

    override fun getAppearanceSettings(): AppearanceSettingsModel {
        return ThemeMapper.mapAppearanceSettings(SharedConfig.bubbleRadius)
    }

    override fun observeAvailableThemes(): Flow<List<ThemeModel>> {
        val themeListFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.themeListUpdated)
        val themeChangedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.didSetNewTheme)

        return merge(themeListFlow, themeChangedFlow)
            .map { getAvailableThemes() }
            .onStart { emit(getAvailableThemes()) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Main)
    }

    override fun getAvailableThemes(): List<ThemeModel> {
        val themesList = Theme.themes ?: return emptyList()
        return themesList.map { ThemeMapper.mapTheme(it) }
    }

    override suspend fun applyTheme(themeKey: String, nightTheme: Boolean): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val themesList = Theme.themes ?: return@withContext Result.Failure(AppError.NotFound("Themes list is empty"))
            val target = themesList.find { it.key == themeKey || it.name == themeKey }
                ?: return@withContext Result.Failure(AppError.NotFound("Theme with key '$themeKey' not found"))

            Theme.applyTheme(target, nightTheme)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to apply theme: ${e.message}", e))
        }
    }

    override suspend fun setNightModeType(type: NightModeType): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Theme.selectedAutoNightType = type.value
            Theme.saveAutoNightThemeConfig()
            Theme.checkAutoNightThemeConditions()
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set night mode type: ${e.message}", e))
        }
    }

    override suspend fun setNightModeSettings(settings: NightModeSettingsModel): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            Theme.selectedAutoNightType = settings.type.value
            Theme.autoNightScheduleByLocation = settings.scheduleByLocation
            Theme.autoNightBrighnessThreshold = settings.brightnessThreshold
            Theme.autoNightDayStartTime = settings.dayStartTime
            Theme.autoNightDayEndTime = settings.dayEndTime
            Theme.autoNightCityName = settings.cityName

            Theme.saveAutoNightThemeConfig()
            Theme.checkAutoNightThemeConditions()
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to update night mode settings: ${e.message}", e))
        }
    }

    override suspend fun setThemeAccent(themeKey: String, accentId: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val themesList = Theme.themes ?: return@withContext Result.Failure(AppError.NotFound("Themes list is empty"))
            val target = themesList.find { it.key == themeKey || it.name == themeKey }
                ?: return@withContext Result.Failure(AppError.NotFound("Theme with key '$themeKey' not found"))

            target.currentAccentId = accentId
            val active = Theme.getActiveTheme()
            if (active != null && (active.key == target.key || active.name == target.name)) {
                Theme.applyTheme(target, Theme.isCurrentThemeNight())
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeAccentListUpdated)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set theme accent: ${e.message}", e))
        }
    }

    override suspend fun setBubbleRadius(radius: Int): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val clamped = radius.coerceIn(0, 30)
            SharedConfig.bubbleRadius = clamped
            val editor: SharedPreferences.Editor = MessagesController.getGlobalMainSettings().edit()
            editor.putInt("bubbleRadius", clamped)
            editor.commit()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to set bubble radius: ${e.message}", e))
        }
    }

    override suspend fun resetToDefault(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            // Reset bubble radius
            SharedConfig.bubbleRadius = 17
            val editor: SharedPreferences.Editor = MessagesController.getGlobalMainSettings().edit()
            editor.putInt("bubbleRadius", 17)
            editor.commit()

            // Reset night mode
            Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_SYSTEM
            Theme.saveAutoNightThemeConfig()
            Theme.checkAutoNightThemeConditions()

            // Reset theme
            val themesList = Theme.themes
            val defaultTheme = themesList?.find { "Blue".equals(it.name, ignoreCase = true) }
            if (defaultTheme != null) {
                Theme.applyTheme(defaultTheme, false)
            }

            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to reset appearance settings: ${e.message}", e))
        }
    }
}
