package org.telegram.messenger.feature.refreshrate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.refreshrate.data.repository.LegacyRefreshRateRepository
import org.telegram.messenger.feature.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.refreshrate.domain.model.RefreshRateDirection
import org.telegram.messenger.feature.refreshrate.domain.model.RefreshRateHysteresisConfig
import org.telegram.messenger.feature.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.refreshrate.domain.usecase.GetDisplayRefreshModesUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.GetRefreshRateStateUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ObserveRefreshRateStateUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.RecordFrameMetricUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ResetRefreshRateStatsUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.SetPreferredRefreshRateModeUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.StartRefreshRateTrackingUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.StopRefreshRateTrackingUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ToggleAdaptiveRefreshRateUseCase
import org.telegram.messenger.feature.refreshrate.presentation.RefreshRateEvent
import org.telegram.messenger.feature.refreshrate.presentation.RefreshRateViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshRateDomainTest {

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
    fun testDisplayRefreshModeModelProperties() {
        val mode60 = DisplayRefreshModeModel(modeId = 1, width = 1080, height = 2400, refreshRate = 60.0f)
        assertTrue(mode60.isApproximately60Hz)
        assertFalse(mode60.isHighRefreshRate)

        val mode120 = DisplayRefreshModeModel(modeId = 2, width = 1080, height = 2400, refreshRate = 120.0f)
        assertFalse(mode120.isApproximately60Hz)
        assertTrue(mode120.isHighRefreshRate)

        val mode90 = DisplayRefreshModeModel(modeId = 3, width = 1080, height = 2400, refreshRate = 90.0f)
        assertFalse(mode90.isApproximately60Hz)
        assertTrue(mode90.isHighRefreshRate)
    }

    @Test
    fun testRefreshRateStateModelCanSwitchCalculation() {
        val mode60 = DisplayRefreshModeModel(modeId = 1, width = 1080, height = 2400, refreshRate = 60.0f)
        val mode120 = DisplayRefreshModeModel(modeId = 2, width = 1080, height = 2400, refreshRate = 120.0f)

        val stateDual = RefreshRateStateModel(mode60 = mode60, modeMax = mode120)
        assertTrue(stateDual.canSwitchRefreshRate)

        val stateSame = RefreshRateStateModel(mode60 = mode60, modeMax = mode60)
        assertFalse(stateSame.canSwitchRefreshRate)

        val stateMissing = RefreshRateStateModel(mode60 = mode60, modeMax = null)
        assertFalse(stateMissing.canSwitchRefreshRate)
    }

    @Test
    fun testRefreshRateHysteresisConfigDefaults() {
        val config = RefreshRateHysteresisConfig()
        assertEquals(1800L, config.stableWindowMs)
        assertEquals(3000L, config.minSwitchIntervalMs)
        assertEquals(55.0f, config.downFpsThreshold, 0.01f)
        assertEquals(58.5f, config.upFpsThreshold, 0.01f)
        assertEquals(240, config.ringSize)
    }

    @Test
    fun testRepositoryTrackingAndModes() {
        val repository = LegacyRefreshRateRepository()
        val initial = repository.getState()
        assertFalse(initial.isTrackingActive)
        assertTrue(initial.isAdaptiveEnabled)
        assertNotNull(initial.currentMode)

        val startRes = repository.startTracking()
        assertTrue(startRes is Result.Success)
        assertTrue(repository.getState().isTrackingActive)

        val toggleRes = repository.setAdaptiveEnabled(false)
        assertTrue(toggleRes is Result.Success)
        assertFalse(repository.getState().isAdaptiveEnabled)

        val stopRes = repository.stopTracking()
        assertTrue(stopRes is Result.Success)
        assertFalse(repository.getState().isTrackingActive)

        val modes = repository.getAvailableModes()
        assertTrue(modes.isNotEmpty())
    }

    @Test
    fun testRepositoryFrameRecordingAndFpsCalculation() {
        val repository = LegacyRefreshRateRepository()
        repository.startTracking()

        // Push 60 frames of 16.666 ms (approx 60 fps: 16_666_666 ns)
        val frameNs = 16_666_666L
        repeat(60) {
            repository.recordFrameDuration(frameNs)
        }

        val state = repository.getState()
        assertEquals(60L, state.totalFramesTracked)
        assertTrue(state.currentFps in 59.0f..61.0f)

        val resetRes = repository.resetStats()
        assertTrue(resetRes is Result.Success)
        assertEquals(0L, repository.getState().totalFramesTracked)
    }

    @Test
    fun testRepositorySetPreferredMode() {
        val repository = LegacyRefreshRateRepository()
        val mode60 = DisplayRefreshModeModel(modeId = 1, width = 1080, height = 2400, refreshRate = 60.0f)

        val res = repository.setPreferredMode(mode60)
        assertTrue(res is Result.Success)
        assertTrue(repository.getState().isPreferring60)
        assertEquals(RefreshRateDirection.DOWN, repository.getState().lastDirection)
    }

    @Test
    fun testUseCases() = runTest(testDispatcher) {
        val repository = LegacyRefreshRateRepository()
        val observeUseCase = ObserveRefreshRateStateUseCase(repository)
        val getStateUseCase = GetRefreshRateStateUseCase(repository)
        val startUseCase = StartRefreshRateTrackingUseCase(repository)
        val stopUseCase = StopRefreshRateTrackingUseCase(repository)
        val toggleAdaptiveUseCase = ToggleAdaptiveRefreshRateUseCase(repository)
        val setPreferredModeUseCase = SetPreferredRefreshRateModeUseCase(repository)
        val recordMetricUseCase = RecordFrameMetricUseCase(repository)
        val resetStatsUseCase = ResetRefreshRateStatsUseCase(repository)
        val getModesUseCase = GetDisplayRefreshModesUseCase(repository)

        assertFalse(getStateUseCase().isTrackingActive)
        startUseCase()
        assertTrue(getStateUseCase().isTrackingActive)

        recordMetricUseCase(16_666_666L)
        assertEquals(1L, getStateUseCase().totalFramesTracked)

        toggleAdaptiveUseCase(false)
        assertFalse(getStateUseCase().isAdaptiveEnabled)

        val mode60 = DisplayRefreshModeModel(1, 1080, 2400, 60.0f)
        setPreferredModeUseCase(mode60)
        assertTrue(getStateUseCase().isPreferring60)

        resetStatsUseCase()
        assertEquals(0L, getStateUseCase().totalFramesTracked)

        stopUseCase()
        assertFalse(getStateUseCase().isTrackingActive)

        val modes = getModesUseCase()
        assertTrue(modes.isNotEmpty())
    }

    @Test
    fun testViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyRefreshRateRepository()
        val viewModel = RefreshRateViewModel(
            observeRefreshRateStateUseCase = ObserveRefreshRateStateUseCase(repository),
            getRefreshRateStateUseCase = GetRefreshRateStateUseCase(repository),
            startRefreshRateTrackingUseCase = StartRefreshRateTrackingUseCase(repository),
            stopRefreshRateTrackingUseCase = StopRefreshRateTrackingUseCase(repository),
            toggleAdaptiveRefreshRateUseCase = ToggleAdaptiveRefreshRateUseCase(repository),
            setPreferredRefreshRateModeUseCase = SetPreferredRefreshRateModeUseCase(repository),
            recordFrameMetricUseCase = RecordFrameMetricUseCase(repository),
            resetRefreshRateStatsUseCase = ResetRefreshRateStatsUseCase(repository)
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTracking)

        viewModel.onEvent(RefreshRateEvent.StartTracking)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isTracking)

        viewModel.onEvent(RefreshRateEvent.RecordFrame(16_666_666L))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.state.totalFramesTracked)

        viewModel.onEvent(RefreshRateEvent.ToggleAdaptive(false))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAdaptive)

        val mode60 = DisplayRefreshModeModel(1, 1080, 2400, 60.0f)
        viewModel.onEvent(RefreshRateEvent.SelectMode(mode60))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.is60Hz)

        viewModel.onEvent(RefreshRateEvent.ResetStats)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0L, viewModel.uiState.value.state.totalFramesTracked)

        viewModel.onEvent(RefreshRateEvent.StopTracking)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTracking)

        viewModel.onEvent(RefreshRateEvent.DismissInfo)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun testAccountFeatureContainerIntegration() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.refreshRateRepository)
        assertNotNull(container.observeRefreshRateStateUseCase)
        assertNotNull(container.getRefreshRateStateUseCase)
        assertNotNull(container.startRefreshRateTrackingUseCase)
        assertNotNull(container.stopRefreshRateTrackingUseCase)
        assertNotNull(container.toggleAdaptiveRefreshRateUseCase)
        assertNotNull(container.setPreferredRefreshRateModeUseCase)
        assertNotNull(container.recordFrameMetricUseCase)
        assertNotNull(container.resetRefreshRateStatsUseCase)
        assertNotNull(container.getDisplayRefreshModesUseCase)

        val vm1 = container.refreshRateViewModel
        val vm2 = container.refreshRateViewModel
        assertEquals(vm1, vm2)

        val createdVm = container.createRefreshRateViewModel()
        assertNotNull(createdVm)
    }
}
