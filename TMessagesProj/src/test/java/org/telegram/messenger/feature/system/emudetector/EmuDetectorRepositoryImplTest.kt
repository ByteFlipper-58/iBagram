package org.telegram.messenger.feature.system.emudetector

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.emudetector.data.datasource.EmuDetectorLocalDataSource
import org.telegram.messenger.feature.system.emudetector.data.datasource.EmuDetectorRemoteDataSource
import org.telegram.messenger.feature.system.emudetector.data.repository.EmuDetectorRepositoryImpl
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.system.emudetector.domain.model.EnvironmentVerdict

class EmuDetectorRepositoryImplTest {

    private lateinit var localDataSource: EmuDetectorLocalDataSource
    private lateinit var remoteDataSource: EmuDetectorRemoteDataSource
    private lateinit var repository: EmuDetectorRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = EmuDetectorLocalDataSource().apply {
            setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.PHYSICAL_DEVICE, simulatedIsEmulator = false)
        }
        remoteDataSource = EmuDetectorRemoteDataSource(currentAccount = 0)
        repository = EmuDetectorRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun initialState_noCachedDiagnostics() {
        assertNull(repository.getCachedDiagnostics())
        assertFalse(repository.isEmulator())
        assertNull(repository.observeDiagnostics().value)
        assertFalse(repository.observeIsEmulator().value)
    }

    @Test
    fun detectEnvironment_physicalDeviceScenario() = runBlocking {
        localDataSource.setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.PHYSICAL_DEVICE, simulatedIsEmulator = false)

        val result = repository.detectEnvironment()
        assertEquals(EnvironmentVerdict.PHYSICAL_DEVICE, result.verdict)
        assertFalse(result.isEmulator)
        assertEquals(0, result.confidenceScore)
        assertTrue(result.triggeredIndicators.isEmpty())

        assertFalse(repository.isEmulator())
        assertNotNull(repository.getCachedDiagnostics())
        assertEquals(EnvironmentVerdict.PHYSICAL_DEVICE, repository.getCachedDiagnostics()?.verdict)
    }

    @Test
    fun detectEnvironment_emulatorScenario() = runBlocking {
        localDataSource.setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.EMULATOR_DETECTED, simulatedIsEmulator = true)

        val result = repository.detectEnvironment()
        assertEquals(EnvironmentVerdict.EMULATOR_DETECTED, result.verdict)
        assertTrue(result.isEmulator)
        assertTrue(result.confidenceScore > 0)
        assertTrue(result.triggeredIndicators.isNotEmpty())

        assertTrue(repository.isEmulator())
        assertEquals(EnvironmentVerdict.EMULATOR_DETECTED, repository.getCachedDiagnostics()?.verdict)
    }

    @Test
    fun detectEnvironment_cachingAndForceRefresh() = runBlocking {
        localDataSource.setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.PHYSICAL_DEVICE, simulatedIsEmulator = false)
        val firstResult = repository.detectEnvironment()

        // Change mode behind the scenes without force refresh
        localDataSource.setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.EMULATOR_DETECTED, simulatedIsEmulator = true)
        val cachedResult = repository.detectEnvironment(forceRefresh = false)
        assertEquals(EnvironmentVerdict.PHYSICAL_DEVICE, cachedResult.verdict)

        // Now force refresh
        val refreshedResult = repository.detectEnvironment(forceRefresh = true)
        assertEquals(EnvironmentVerdict.EMULATOR_DETECTED, refreshedResult.verdict)
        assertTrue(refreshedResult.isEmulator)
    }

    @Test
    fun configUpdates_andAddCustomPackage() {
        val originalConfig = repository.getConfig()
        assertTrue(originalConfig.checkTelephony)

        val updatedConfig = originalConfig.copy(checkTelephony = false, confidenceThreshold = 5)
        repository.updateConfig(updatedConfig)
        assertEquals(false, repository.getConfig().checkTelephony)
        assertEquals(5, repository.getConfig().confidenceThreshold)

        repository.addCustomPackage("com.example.custom.emu")
        assertTrue(repository.getConfig().customPackageNames.contains("com.example.custom.emu"))
    }

    @Test
    fun clearCache_resetsCachedDataAndFlows() = runBlocking {
        localDataSource.setTestMode(isTest = true, simulatedVerdict = EnvironmentVerdict.EMULATOR_DETECTED, simulatedIsEmulator = true)
        repository.detectEnvironment()
        assertNotNull(repository.getCachedDiagnostics())
        assertTrue(repository.observeIsEmulator().value)

        repository.clearCache()
        assertNull(repository.getCachedDiagnostics())
        assertFalse(repository.observeIsEmulator().value)
    }
}
