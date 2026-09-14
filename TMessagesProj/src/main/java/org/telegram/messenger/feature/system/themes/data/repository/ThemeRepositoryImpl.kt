package org.telegram.messenger.feature.system.themes.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.data.datasource.ThemesLocalDataSource
import org.telegram.messenger.feature.system.themes.data.datasource.ThemesRemoteDataSource
import org.telegram.messenger.feature.system.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository

/**
 * Clean repository implementation coordinating local appearance state and remote theme synchronization.
 */
class ThemeRepositoryImpl(
    private val localDataSource: ThemesLocalDataSource,
    private val remoteDataSource: ThemesRemoteDataSource
) : ThemeRepository {

    override fun observeAppearanceSettings(): Flow<AppearanceSettingsModel> {
        return localDataSource.observeAppearanceSettings()
    }

    override fun getAppearanceSettings(): AppearanceSettingsModel {
        return localDataSource.getAppearanceSettings()
    }

    override fun observeAvailableThemes(): Flow<List<ThemeModel>> {
        return localDataSource.observeAvailableThemes()
    }

    override fun getAvailableThemes(): List<ThemeModel> {
        return localDataSource.getAvailableThemes()
    }

    override suspend fun applyTheme(themeKey: String, nightTheme: Boolean): Result<Unit> {
        return localDataSource.applyTheme(themeKey, nightTheme)
    }

    override suspend fun setNightModeType(type: NightModeType): Result<Unit> {
        return localDataSource.setNightModeType(type)
    }

    override suspend fun setNightModeSettings(settings: NightModeSettingsModel): Result<Unit> {
        return localDataSource.setNightModeSettings(settings)
    }

    override suspend fun setThemeAccent(themeKey: String, accentId: Int): Result<Unit> {
        return localDataSource.setThemeAccent(themeKey, accentId)
    }

    override suspend fun setBubbleRadius(radius: Int): Result<Unit> {
        return localDataSource.setBubbleRadius(radius)
    }

    override suspend fun resetToDefault(): Result<Unit> {
        return localDataSource.resetToDefault()
    }
}
