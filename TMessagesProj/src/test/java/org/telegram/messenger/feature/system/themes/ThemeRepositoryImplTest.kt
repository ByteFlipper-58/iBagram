package org.telegram.messenger.feature.system.themes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.data.datasource.ThemesLocalDataSource
import org.telegram.messenger.feature.system.themes.data.datasource.ThemesRemoteDataSource
import org.telegram.messenger.feature.system.themes.data.repository.ThemeRepositoryImpl
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType

class ThemeRepositoryImplTest {

    private lateinit var localDataSource: ThemesLocalDataSource
    private lateinit var remoteDataSource: ThemesRemoteDataSource
    private lateinit var repository: ThemeRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ThemesLocalDataSource(currentAccount = 0, testMode = true)
        remoteDataSource = ThemesRemoteDataSource(currentAccount = 0)
        repository = ThemeRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testAppearanceSettingsObservation() = runTest {
        val initial = repository.getAppearanceSettings()
        assertNotNull(initial)

        val flowItem = repository.observeAppearanceSettings().first()
        assertNotNull(flowItem)

        val radiusRes = repository.setBubbleRadius(24)
        assertTrue(radiusRes is Result.Success)
        assertEquals(24, repository.getAppearanceSettings().bubbleRadius)
    }

    @Test
    fun testAvailableThemes() = runTest {
        val themes = repository.getAvailableThemes()
        assertTrue(themes.isNotEmpty())

        val flowThemes = repository.observeAvailableThemes().first()
        assertEquals(themes.size, flowThemes.size)
    }

    @Test
    fun testApplyTheme() = runTest {
        val applyRes = repository.applyTheme("Dark Blue", nightTheme = true)
        assertTrue(applyRes is Result.Success)

        val current = repository.getAppearanceSettings()
        assertEquals("Dark Blue", current.currentTheme.key)
        assertTrue(current.currentTheme.isDark)
    }

    @Test
    fun testNightModeSettings() = runTest {
        val typeRes = repository.setNightModeType(NightModeType.SCHEDULED)
        assertTrue(typeRes is Result.Success)
        assertEquals(NightModeType.SCHEDULED, repository.getAppearanceSettings().nightModeSettings.type)

        val newSettings = NightModeSettingsModel(
            type = NightModeType.AUTOMATIC,
            brightnessThreshold = 0.45f
        )
        val settingsRes = repository.setNightModeSettings(newSettings)
        assertTrue(settingsRes is Result.Success)
        assertEquals(NightModeType.AUTOMATIC, repository.getAppearanceSettings().nightModeSettings.type)
        assertEquals(0.45f, repository.getAppearanceSettings().nightModeSettings.brightnessThreshold, 0.001f)
    }

    @Test
    fun testResetToDefault() = runTest {
        repository.setBubbleRadius(30)
        assertEquals(30, repository.getAppearanceSettings().bubbleRadius)

        val resetRes = repository.resetToDefault()
        assertTrue(resetRes is Result.Success)
        assertEquals(17, repository.getAppearanceSettings().bubbleRadius)
    }

    @Test
    fun testRemoteThemesSync() = runTest {
        val remoteRes = remoteDataSource.getThemes()
        assertTrue(remoteRes is Result.Success)
    }
}
