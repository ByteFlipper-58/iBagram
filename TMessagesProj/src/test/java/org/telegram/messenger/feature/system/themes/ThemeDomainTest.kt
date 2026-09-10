package org.telegram.messenger.feature.system.themes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.themes.data.mapper.ThemeMapper
import org.telegram.messenger.feature.system.themes.domain.model.AppearanceSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeSettingsModel
import org.telegram.messenger.feature.system.themes.domain.model.NightModeType
import org.telegram.messenger.feature.system.themes.domain.model.ThemeAccentModel
import org.telegram.messenger.feature.system.themes.domain.model.ThemeModel
import org.telegram.messenger.feature.system.themes.domain.model.WallpaperModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository
import org.telegram.messenger.feature.system.themes.domain.usecase.ApplyThemeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.GetAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.GetAvailableThemesUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveAvailableThemesUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveNightModeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ResetAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetBubbleRadiusUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetNightModeSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetNightModeTypeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetThemeAccentUseCase
import org.telegram.messenger.feature.system.themes.presentation.ThemeEvent
import org.telegram.messenger.feature.system.themes.presentation.ThemeViewModel
import org.telegram.ui.ActionBar.Theme
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDomainModels() {
        // 1. NightModeType
        assertEquals(NightModeType.NONE, NightModeType.fromValue(0))
        assertEquals(NightModeType.SCHEDULED, NightModeType.fromValue(1))
        assertEquals(NightModeType.AUTOMATIC, NightModeType.fromValue(2))
        assertEquals(NightModeType.SYSTEM, NightModeType.fromValue(3))
        assertEquals(NightModeType.NONE, NightModeType.fromValue(999))

        // 2. ThemeAccentModel
        val accent = ThemeAccentModel(
            id = 99,
            accentColor = 0x0088cc,
            accentColor2 = 0x005588,
            myMessagesAccentColor = 0x2299ee,
            isDefault = true
        )
        assertEquals(99, accent.id)
        assertEquals(0x0088cc, accent.accentColor)
        assertTrue(accent.isDefault)

        // 3. ThemeModel
        val theme = ThemeModel(
            key = "Blue",
            name = "Classic",
            isDark = false,
            isDefault = true,
            accents = listOf(accent),
            currentAccentId = 99,
            previewColor = 0x0088cc
        )
        assertEquals("Blue", theme.key)
        assertEquals("Classic", theme.name)
        assertFalse(theme.isDark)
        assertTrue(theme.isDefault)
        assertEquals(1, theme.accents.size)

        // 4. NightModeSettingsModel
        val nightSettings = NightModeSettingsModel(
            type = NightModeType.SCHEDULED,
            scheduleByLocation = true,
            brightnessThreshold = 0.35f,
            dayStartTime = 1320,
            dayEndTime = 480,
            cityName = "Berlin"
        )
        assertEquals(NightModeType.SCHEDULED, nightSettings.type)
        assertTrue(nightSettings.scheduleByLocation)
        assertEquals(0.35f, nightSettings.brightnessThreshold)
        assertEquals("Berlin", nightSettings.cityName)

        // 5. WallpaperModel
        val wallpaper = WallpaperModel(
            slug = "custom_slug",
            isDefault = false,
            isFromTheme = true
        )
        assertEquals("custom_slug", wallpaper.slug)
        assertFalse(wallpaper.isDefault)
        assertTrue(wallpaper.isFromTheme)

        // 6. AppearanceSettingsModel
        val appearance = AppearanceSettingsModel(
            currentTheme = theme,
            currentNightTheme = null,
            isNightModeActive = false,
            nightModeSettings = nightSettings,
            bubbleRadius = 17,
            wallpaper = wallpaper
        )
        assertEquals(theme, appearance.currentTheme)
        assertNull(appearance.currentNightTheme)
        assertFalse(appearance.isNightModeActive)
        assertEquals(17, appearance.bubbleRadius)
    }

    @Test
    fun testThemeMapper() {
        // Test mapTheme(null)
        val defaultTheme = ThemeMapper.mapTheme(null)
        assertEquals("default", defaultTheme.key)
        assertEquals("Default", defaultTheme.name)
        assertTrue(defaultTheme.isDefault)
        assertFalse(defaultTheme.isDark)

        // Test mapThemeAccent(null)
        val nullAccent = ThemeMapper.mapThemeAccent(null)
        assertNull(nullAccent)

        // Test mapTheme with mock ThemeInfo
        val info = Theme.ThemeInfo::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        info.name = "Custom Theme"
        info.currentAccentId = 101
        info.accentBaseColor = 0x112233

        val mappedTheme = ThemeMapper.mapTheme(info)
        assertEquals("Custom Theme", mappedTheme.key)
        assertEquals("Custom Theme", mappedTheme.name)
        assertEquals(101, mappedTheme.currentAccentId)
        assertEquals(0x112233, mappedTheme.previewColor)
    }

    @Test
    fun testUseCasesAndRepository() = runTest(testDispatcher) {
        val fakeRepo = FakeThemeRepository()

        val observeAppearance = ObserveAppearanceSettingsUseCase(fakeRepo)
        val getAppearance = GetAppearanceSettingsUseCase(fakeRepo)
        val observeThemes = ObserveAvailableThemesUseCase(fakeRepo)
        val getThemes = GetAvailableThemesUseCase(fakeRepo)
        val applyTheme = ApplyThemeUseCase(fakeRepo)
        val observeNightMode = ObserveNightModeUseCase(fakeRepo)
        val setNightModeType = SetNightModeTypeUseCase(fakeRepo)
        val setNightModeSettings = SetNightModeSettingsUseCase(fakeRepo)
        val setThemeAccent = SetThemeAccentUseCase(fakeRepo)
        val setBubbleRadius = SetBubbleRadiusUseCase(fakeRepo)
        val resetAppearance = ResetAppearanceSettingsUseCase(fakeRepo)

        // Initial checks
        val initialAppearance = getAppearance()
        assertEquals("Blue", initialAppearance.currentTheme.key)
        assertEquals(17, initialAppearance.bubbleRadius)
        assertEquals(NightModeType.NONE, initialAppearance.nightModeSettings.type)

        val initialThemes = getThemes()
        assertEquals(2, initialThemes.size)

        // Test applyTheme
        val applyRes = applyTheme("Dark Blue", true)
        assertTrue(applyRes is Result.Success)
        assertEquals("Dark Blue", fakeRepo.currentThemeKey)
        assertTrue(fakeRepo.isNight)

        // Test setNightModeType
        val nightRes = setNightModeType(NightModeType.SYSTEM)
        assertTrue(nightRes is Result.Success)
        assertEquals(NightModeType.SYSTEM, fakeRepo.nightModeType)

        // Test setNightModeSettings
        val updatedSettings = NightModeSettingsModel(
            type = NightModeType.SCHEDULED,
            scheduleByLocation = true,
            cityName = "Paris"
        )
        val settingsRes = setNightModeSettings(updatedSettings)
        assertTrue(settingsRes is Result.Success)
        assertEquals(NightModeType.SCHEDULED, fakeRepo.nightModeSettings.type)
        assertEquals("Paris", fakeRepo.nightModeSettings.cityName)

        // Test setThemeAccent
        val accentRes = setThemeAccent("Blue", 102)
        assertTrue(accentRes is Result.Success)
        assertEquals(102, fakeRepo.lastAccentId)

        // Test setBubbleRadius
        val bubbleRes = setBubbleRadius(22)
        assertTrue(bubbleRes is Result.Success)
        assertEquals(22, fakeRepo.bubbleRadius)

        // Test resetAppearance
        val resetRes = resetAppearance()
        assertTrue(resetRes is Result.Success)
        assertEquals(17, fakeRepo.bubbleRadius)
        assertEquals(NightModeType.NONE, fakeRepo.nightModeType)
        assertEquals("Blue", fakeRepo.currentThemeKey)

        // Verify observe streams
        val observedAppearance = observeAppearance().first()
        assertNotNull(observedAppearance)

        val observedThemes = observeThemes().first()
        assertEquals(2, observedThemes.size)

        val observedNight = observeNightMode().first()
        assertNotNull(observedNight)
    }

    @Test
    fun testViewModelStateAndEvents() = runTest(testDispatcher) {
        val fakeRepo = FakeThemeRepository()

        val viewModel = ThemeViewModel(
            themeRepository = fakeRepo,
            observeAppearanceSettingsUseCase = ObserveAppearanceSettingsUseCase(fakeRepo),
            getAppearanceSettingsUseCase = GetAppearanceSettingsUseCase(fakeRepo),
            observeAvailableThemesUseCase = ObserveAvailableThemesUseCase(fakeRepo),
            getAvailableThemesUseCase = GetAvailableThemesUseCase(fakeRepo),
            applyThemeUseCase = ApplyThemeUseCase(fakeRepo),
            observeNightModeUseCase = ObserveNightModeUseCase(fakeRepo),
            setNightModeTypeUseCase = SetNightModeTypeUseCase(fakeRepo),
            setNightModeSettingsUseCase = SetNightModeSettingsUseCase(fakeRepo),
            setThemeAccentUseCase = SetThemeAccentUseCase(fakeRepo),
            setBubbleRadiusUseCase = SetBubbleRadiusUseCase(fakeRepo),
            resetAppearanceSettingsUseCase = ResetAppearanceSettingsUseCase(fakeRepo)
        )

        testScheduler.advanceUntilIdle()

        // 1. Initial State
        val initialState = viewModel.uiState.value
        assertEquals("Blue", initialState.currentTheme?.key)
        assertEquals(17, initialState.bubbleRadius)
        assertEquals(2, initialState.availableThemes.size)

        // 2. Select theme
        viewModel.onEvent(ThemeEvent.SelectTheme("Dark Blue", true))
        testScheduler.advanceUntilIdle()
        assertEquals("Theme applied", viewModel.uiState.value.userMessage)
        assertEquals("Dark Blue", fakeRepo.currentThemeKey)

        // 3. Clear message
        viewModel.onEvent(ThemeEvent.ClearMessage)
        assertNull(viewModel.uiState.value.userMessage)

        // 4. Select accent
        viewModel.onEvent(ThemeEvent.SelectThemeAccent("Dark Blue", 105))
        testScheduler.advanceUntilIdle()
        assertEquals("Accent updated", viewModel.uiState.value.userMessage)
        assertEquals(105, fakeRepo.lastAccentId)

        // 5. Set Night Mode
        viewModel.onEvent(ThemeEvent.SetNightMode(NightModeType.AUTOMATIC))
        testScheduler.advanceUntilIdle()
        assertEquals("Night mode updated", viewModel.uiState.value.userMessage)
        assertEquals(NightModeType.AUTOMATIC, fakeRepo.nightModeType)

        // 6. Update Night Mode Settings
        val customNight = NightModeSettingsModel(
            type = NightModeType.SCHEDULED,
            scheduleByLocation = true,
            cityName = "Tokyo"
        )
        viewModel.onEvent(ThemeEvent.UpdateNightModeSettings(customNight))
        testScheduler.advanceUntilIdle()
        assertEquals("Night mode settings saved", viewModel.uiState.value.userMessage)
        assertEquals("Tokyo", fakeRepo.nightModeSettings.cityName)

        // 7. Set bubble radius
        viewModel.onEvent(ThemeEvent.SetBubbleRadius(25))
        testScheduler.advanceUntilIdle()
        assertEquals(25, viewModel.uiState.value.bubbleRadius)
        assertEquals(25, fakeRepo.bubbleRadius)

        // 8. Reset to defaults
        viewModel.onEvent(ThemeEvent.ResetToDefaults)
        testScheduler.advanceUntilIdle()
        assertEquals("Reset to defaults", viewModel.uiState.value.userMessage)
        assertEquals(17, fakeRepo.bubbleRadius)
    }

    private class FakeThemeRepository : ThemeRepository {
        var currentThemeKey: String = "Blue"
        var isNight: Boolean = false
        var lastAccentId: Int = 99
        var nightModeType: NightModeType = NightModeType.NONE
        var nightModeSettings: NightModeSettingsModel = NightModeSettingsModel()
        var bubbleRadius: Int = 17

        private val defaultTheme = ThemeModel(
            key = "Blue",
            name = "Classic",
            isDark = false,
            isDefault = true,
            currentAccentId = 99
        )

        private val darkTheme = ThemeModel(
            key = "Dark Blue",
            name = "Night",
            isDark = true,
            isDefault = false,
            currentAccentId = 100
        )

        private val appearanceFlow = MutableSharedFlow<AppearanceSettingsModel>(replay = 1)
        private val themesFlow = MutableSharedFlow<List<ThemeModel>>(replay = 1)

        init {
            emitState()
        }

        private fun emitState() {
            val current = if (currentThemeKey == "Blue") defaultTheme else darkTheme
            appearanceFlow.tryEmit(
                AppearanceSettingsModel(
                    currentTheme = current,
                    currentNightTheme = darkTheme,
                    isNightModeActive = isNight,
                    nightModeSettings = nightModeSettings,
                    bubbleRadius = bubbleRadius,
                    wallpaper = WallpaperModel()
                )
            )
            themesFlow.tryEmit(listOf(defaultTheme, darkTheme))
        }

        override fun observeAppearanceSettings(): Flow<AppearanceSettingsModel> = appearanceFlow

        override fun getAppearanceSettings(): AppearanceSettingsModel {
            val current = if (currentThemeKey == "Blue") defaultTheme else darkTheme
            return AppearanceSettingsModel(
                currentTheme = current,
                currentNightTheme = darkTheme,
                isNightModeActive = isNight,
                nightModeSettings = nightModeSettings,
                bubbleRadius = bubbleRadius,
                wallpaper = WallpaperModel()
            )
        }

        override fun observeAvailableThemes(): Flow<List<ThemeModel>> = themesFlow

        override fun getAvailableThemes(): List<ThemeModel> = listOf(defaultTheme, darkTheme)

        override suspend fun applyTheme(themeKey: String, nightTheme: Boolean): Result<Unit> {
            currentThemeKey = themeKey
            isNight = nightTheme
            emitState()
            return Result.Success(Unit)
        }

        override suspend fun setNightModeType(type: NightModeType): Result<Unit> {
            nightModeType = type
            nightModeSettings = nightModeSettings.copy(type = type)
            emitState()
            return Result.Success(Unit)
        }

        override suspend fun setNightModeSettings(settings: NightModeSettingsModel): Result<Unit> {
            nightModeSettings = settings
            nightModeType = settings.type
            emitState()
            return Result.Success(Unit)
        }

        override suspend fun setThemeAccent(themeKey: String, accentId: Int): Result<Unit> {
            lastAccentId = accentId
            emitState()
            return Result.Success(Unit)
        }

        override suspend fun setBubbleRadius(radius: Int): Result<Unit> {
            bubbleRadius = radius
            emitState()
            return Result.Success(Unit)
        }

        override suspend fun resetToDefault(): Result<Unit> {
            currentThemeKey = "Blue"
            isNight = false
            nightModeType = NightModeType.NONE
            nightModeSettings = NightModeSettingsModel()
            bubbleRadius = 17
            emitState()
            return Result.Success(Unit)
        }
    }
}
