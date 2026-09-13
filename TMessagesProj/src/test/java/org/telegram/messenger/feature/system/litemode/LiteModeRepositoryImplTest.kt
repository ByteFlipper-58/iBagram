package org.telegram.messenger.feature.system.litemode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.litemode.data.datasource.LiteModeLocalDataSource
import org.telegram.messenger.feature.system.litemode.data.datasource.LiteModeRemoteDataSource
import org.telegram.messenger.feature.system.litemode.data.repository.LiteModeRepositoryImpl
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeState

@OptIn(ExperimentalCoroutinesApi::class)
class LiteModeRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: LiteModeLocalDataSource
    private lateinit var remoteDataSource: LiteModeRemoteDataSource
    private lateinit var repository: LiteModeRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = LiteModeLocalDataSource(currentAccount = 0).apply {
            setTestMode(
                LiteModeState(
                    rawFlags = LiteModePreset.HIGH.value,
                    powerSaverThreshold = 10,
                    batteryLevel = 100,
                    isPowerSaverActive = false,
                    isTablet = false,
                    hasPremium = false
                )
            )
        }
        remoteDataSource = LiteModeRemoteDataSource(currentAccount = 0)
        repository = LiteModeRepositoryImpl(
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @Test
    fun testDefaultState() {
        val state = repository.getLiteModeState()
        assertEquals(LiteModePreset.HIGH.value, state.rawFlags)
        assertEquals(10, state.powerSaverThreshold)
        assertEquals(100, state.batteryLevel)
        assertFalse(state.isPowerSaverActive)
        assertEquals(LiteModePreset.HIGH, state.preset)
    }

    @Test
    fun testSetFlagEnabled() = runTest(testDispatcher) {
        val flag = LiteModeFlag.CHAT_BACKGROUND
        repository.setFlagEnabled(flag, false)
        var state = repository.getLiteModeState()
        assertEquals(0, state.rawFlags and flag.bitMask)

        repository.setFlagEnabled(flag, true)
        state = repository.getLiteModeState()
        assertEquals(flag.bitMask, state.rawFlags and flag.bitMask)
    }

    @Test
    fun testSetAllFlags() = runTest(testDispatcher) {
        repository.setAllFlags(12345)
        val state = repository.getLiteModeState()
        assertEquals(12345, state.rawFlags)
    }

    @Test
    fun testApplyPreset() = runTest(testDispatcher) {
        repository.applyPreset(LiteModePreset.LOW)
        var state = repository.getLiteModeState()
        assertEquals(LiteModePreset.LOW.value, state.rawFlags)
        assertEquals(LiteModePreset.LOW, state.preset)

        repository.applyPreset(LiteModePreset.MEDIUM)
        state = repository.getLiteModeState()
        assertEquals(LiteModePreset.MEDIUM.value, state.rawFlags)
        assertEquals(LiteModePreset.MEDIUM, state.preset)

        repository.applyPreset(LiteModePreset.HIGH)
        state = repository.getLiteModeState()
        assertEquals(LiteModePreset.HIGH.value, state.rawFlags)
        assertEquals(LiteModePreset.HIGH, state.preset)
    }

    @Test
    fun testSetPowerSaverThreshold() = runTest(testDispatcher) {
        repository.setPowerSaverThreshold(25)
        var state = repository.getLiteModeState()
        assertEquals(25, state.powerSaverThreshold)
        assertFalse(state.isPowerSaverActive)

        repository.updateBatteryLevel(20)
        state = repository.getLiteModeState()
        assertTrue(state.isPowerSaverActive)
    }

    @Test
    fun testUpdateBatteryLevel() = runTest(testDispatcher) {
        repository.setPowerSaverThreshold(15)
        repository.updateBatteryLevel(10)
        var state = repository.getLiteModeState()
        assertEquals(10, state.batteryLevel)
        assertTrue(state.isPowerSaverActive)

        repository.updateBatteryLevel(80)
        state = repository.getLiteModeState()
        assertEquals(80, state.batteryLevel)
        assertFalse(state.isPowerSaverActive)
    }

    @Test
    fun testObserveLiteModeState() = runTest(testDispatcher) {
        val initial = repository.observeLiteModeState().first()
        assertEquals(LiteModePreset.HIGH.value, initial.rawFlags)

        repository.applyPreset(LiteModePreset.LOW)
        val updated = repository.observeLiteModeState().first()
        assertEquals(LiteModePreset.LOW.value, updated.rawFlags)
    }
}
