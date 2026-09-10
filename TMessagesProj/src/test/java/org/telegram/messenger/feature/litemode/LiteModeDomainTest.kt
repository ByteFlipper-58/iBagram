package org.telegram.messenger.feature.litemode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.litemode.data.mapper.LiteModeMapper
import org.telegram.messenger.feature.litemode.data.repository.LegacyLiteModeRepository
import org.telegram.messenger.feature.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.litemode.domain.model.LiteModeState
import org.telegram.messenger.feature.litemode.domain.usecase.CalculateEffectiveFlagsUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.CheckLiteModeFlagUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.ObserveLiteModeStateUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.ResolvePresetUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.SetLiteModePresetUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.ToggleLiteModeFlagUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.UpdatePowerSaverThresholdUseCase
import org.telegram.messenger.feature.litemode.presentation.LiteModeEvent
import org.telegram.messenger.feature.litemode.presentation.LiteModeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class LiteModeDomainTest {

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
    fun testCalculateEffectiveFlagsPowerSaver() {
        val calculate = CalculateEffectiveFlagsUseCase()

        // 1. Power saver active -> flags are 0
        val effectivePowerSaver = calculate(
            rawFlags = LiteModePreset.HIGH.value,
            isPowerSaverActive = true,
            hasPremium = false,
            isTablet = false
        )
        assertEquals(0, effectivePowerSaver)

        // 2. Power saver inactive -> flags match raw flags
        val effectiveNormal = calculate(
            rawFlags = LiteModePreset.HIGH.value,
            isPowerSaverActive = false,
            hasPremium = false,
            isTablet = false
        )
        assertTrue(effectiveNormal > 0)
    }

    @Test
    fun testPresetDetectionAndMapper() {
        val resolve = ResolvePresetUseCase()

        assertEquals(LiteModePreset.HIGH, resolve(LiteModePreset.HIGH.value))
        assertEquals(LiteModePreset.MEDIUM, resolve(LiteModePreset.MEDIUM.value))
        assertEquals(LiteModePreset.LOW, resolve(LiteModePreset.LOW.value))
        assertEquals(LiteModePreset.POWER_SAVER, resolve(LiteModePreset.POWER_SAVER.value))
        assertEquals(LiteModePreset.CUSTOM, resolve(12345))

        // Mapper round-trip
        assertEquals(LiteModePreset.HIGH, LiteModeMapper.mapLegacyToPreset(LiteModePreset.HIGH.value))
        assertEquals(LiteModePreset.LOW, LiteModeMapper.mapLegacyToPreset(LiteModePreset.LOW.value))
    }

    @Test
    fun testPremiumEmojiPreprocessing() {
        val calculate = CalculateEffectiveFlagsUseCase()
        val rawWithEmoji = LiteModeFlag.ANIMATED_EMOJI_KEYBOARD_PREMIUM.bitMask or LiteModeFlag.ANIMATED_EMOJI_KEYBOARD_NOT_PREMIUM.bitMask

        // Premium user -> gets PREMIUM bit (4)
        val flagsPremium = calculate(
            rawFlags = rawWithEmoji,
            isPowerSaverActive = false,
            hasPremium = true,
            isTablet = false
        )
        assertTrue((flagsPremium and 4) != 0)
        assertEquals(0, flagsPremium and 16384)

        // Non-premium user -> gets NOT_PREMIUM bit (16384)
        val flagsNonPremium = calculate(
            rawFlags = rawWithEmoji,
            isPowerSaverActive = false,
            hasPremium = false,
            isTablet = false
        )
        assertEquals(0, flagsNonPremium and 4)
        assertTrue((flagsNonPremium and 16384) != 0)
    }

    @Test
    fun testTabletTwoColumnForumException() {
        val checkFlag = CheckLiteModeFlagUseCase()

        // Phone without flag set
        val phoneState = LiteModeState(
            rawFlags = 0,
            isPowerSaverActive = false,
            isTablet = false
        )
        assertFalse(checkFlag(phoneState, LiteModeFlag.CHAT_FORUM_TWOCOLUMN))

        // Tablet without flag set -> must STILL be enabled
        val tabletState = LiteModeState(
            rawFlags = 0,
            isPowerSaverActive = false,
            isTablet = true
        )
        assertTrue(checkFlag(tabletState, LiteModeFlag.CHAT_FORUM_TWOCOLUMN))
    }

    @Test
    fun testLiteModeViewModelMviLifecycle() = runTest(testDispatcher) {
        val repository = LegacyLiteModeRepository(
            currentAccount = 0,
            mainDispatcher = testDispatcher
        )
        val observeState = ObserveLiteModeStateUseCase(repository)
        val toggleFlag = ToggleLiteModeFlagUseCase(repository)
        val setPreset = SetLiteModePresetUseCase(repository)
        val updateThreshold = UpdatePowerSaverThresholdUseCase(repository)

        val viewModel = LiteModeViewModel(
            observeLiteModeState = observeState,
            toggleLiteModeFlag = toggleFlag,
            setLiteModePreset = setPreset,
            updatePowerSaverThreshold = updateThreshold,
            repository = repository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Initial state
        assertEquals(LiteModePreset.HIGH, viewModel.uiState.value.currentPreset)
        assertFalse(viewModel.uiState.value.isPowerSaverActive)

        // 2. Set preset to LOW
        viewModel.onEvent(LiteModeEvent.ApplyPreset(LiteModePreset.LOW))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(LiteModePreset.LOW, viewModel.uiState.value.currentPreset)

        // 3. Toggle flag
        viewModel.onEvent(LiteModeEvent.SetFlag(LiteModeFlag.CHAT_BLUR, true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFlagToggledInSettings(LiteModeFlag.CHAT_BLUR))

        // 4. Set power saver threshold to 20%
        viewModel.onEvent(LiteModeEvent.SetPowerSaverThreshold(20))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.powerSaverThreshold)

        // 5. Simulate battery drain to 15% -> power saver should activate!
        viewModel.onEvent(LiteModeEvent.BatteryLevelChanged(15))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPowerSaverActive)

        // 6. Battery charge to 80% -> power saver turns off!
        viewModel.onEvent(LiteModeEvent.BatteryLevelChanged(80))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isPowerSaverActive)
    }
}
