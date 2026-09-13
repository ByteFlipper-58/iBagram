package org.telegram.messenger.feature.system.refreshrate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.data.datasource.RefreshRateLocalDataSource
import org.telegram.messenger.feature.system.refreshrate.data.datasource.RefreshRateRemoteDataSource
import org.telegram.messenger.feature.system.refreshrate.data.repository.RefreshRateRepositoryImpl
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateDirection
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateHysteresisConfig

class RefreshRateRepositoryImplTest {

    private lateinit var localDataSource: RefreshRateLocalDataSource
    private lateinit var remoteDataSource: RefreshRateRemoteDataSource
    private lateinit var repository: RefreshRateRepositoryImpl

    @Before
    fun setUp() {
        val config = RefreshRateHysteresisConfig(
            stableWindowMs = 100L,
            minSwitchIntervalMs = 200L,
            downFpsThreshold = 55.0f,
            upFpsThreshold = 58.5f,
            ringSize = 10
        )
        localDataSource = RefreshRateLocalDataSource(config)
        remoteDataSource = RefreshRateRemoteDataSource()
        repository = RefreshRateRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testInitialState() = runTest {
        val state = repository.getState()
        assertFalse(state.isTrackingActive)
        assertTrue(state.isAdaptiveEnabled)
        assertNotNull(state.mode60)
        assertNotNull(state.modeMax)
        assertEquals(0L, state.totalFramesTracked)
        assertEquals(2, repository.getAvailableModes().size)
    }

    @Test
    fun testStartAndStopTracking() = runTest {
        val startResult = repository.startTracking()
        assertTrue(startResult is Result.Success)
        assertTrue(repository.getState().isTrackingActive)

        val stopResult = repository.stopTracking()
        assertTrue(stopResult is Result.Success)
        assertFalse(repository.getState().isTrackingActive)
    }

    @Test
    fun testSetAdaptiveEnabled() = runTest {
        val resDisable = repository.setAdaptiveEnabled(false)
        assertTrue(resDisable is Result.Success)
        assertFalse(repository.getState().isAdaptiveEnabled)

        val resEnable = repository.setAdaptiveEnabled(true)
        assertTrue(resEnable is Result.Success)
        assertTrue(repository.getState().isAdaptiveEnabled)
    }

    @Test
    fun testSetPreferredMode() = runTest {
        val mode60 = repository.getState().mode60 ?: DisplayRefreshModeModel(1, 1080, 2400, 60.0f)
        val result = repository.setPreferredMode(mode60)
        assertTrue(result is Result.Success)

        val state = repository.getState()
        assertEquals(mode60, state.currentMode)
        assertTrue(state.isPreferring60)
        assertEquals(RefreshRateDirection.DOWN, state.lastDirection)
    }

    @Test
    fun testRecordFrameDuration() = runTest {
        repository.startTracking()

        // 16.6ms per frame = 60 FPS
        val durationNs = 16_666_666L
        for (i in 1..5) {
            val res = repository.recordFrameDuration(durationNs)
            assertTrue(res is Result.Success)
        }

        val state = repository.getState()
        assertEquals(5L, state.totalFramesTracked)
        assertTrue(state.currentFps > 55.0f && state.currentFps < 65.0f)
    }

    @Test
    fun testResetStats() = runTest {
        repository.startTracking()
        repository.recordFrameDuration(16_666_666L)
        repository.recordFrameDuration(16_666_666L)
        assertEquals(2L, repository.getState().totalFramesTracked)

        val res = repository.resetStats()
        assertTrue(res is Result.Success)
        assertEquals(0L, repository.getState().totalFramesTracked)
    }
}
