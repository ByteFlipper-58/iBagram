package org.telegram.messenger.feature.security.biometrics

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
import org.telegram.messenger.FingerprintController
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.biometrics.data.datasource.BiometricsLocalDataSource
import org.telegram.messenger.feature.security.biometrics.data.repository.BiometricsRepositoryImpl
import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricStatus

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeBiometricsLocalDataSource
    private lateinit var repository: BiometricsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeBiometricsLocalDataSource()
        repository = BiometricsRepositoryImpl(
            localDataSource = fakeLocalDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetKeyStateWhenNotSupported() = runTest {
        fakeLocalDataSource.supported = false
        val state = repository.getKeyState()

        assertFalse(state.isKeyReady)
        assertFalse(state.hasChangedFingerprints)
        assertEquals(BiometricStatus.NotSupported, state.status)
        assertFalse(state.canAuthenticate)
    }

    @Test
    fun testGetKeyStateWhenHardwareUnavailable() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.hardwareDetected = false
        val state = repository.getKeyState()

        assertEquals(BiometricStatus.HardwareUnavailable, state.status)
        assertFalse(state.canAuthenticate)
    }

    @Test
    fun testGetKeyStateWhenNoEnrolledBiometrics() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.hardwareDetected = true
        fakeLocalDataSource.enrolledBiometrics = false
        val state = repository.getKeyState()

        assertEquals(BiometricStatus.NoEnrolledBiometrics, state.status)
        assertFalse(state.canAuthenticate)
    }

    @Test
    fun testGetKeyStateWhenKeyPermanentlyInvalidated() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.hardwareDetected = true
        fakeLocalDataSource.enrolledBiometrics = true
        fakeLocalDataSource.deviceBiometricsChanged = true
        fakeLocalDataSource.keyReady = true
        val state = repository.getKeyState()

        assertEquals(BiometricStatus.KeyPermanentlyInvalidated, state.status)
        assertTrue(state.hasChangedFingerprints)
        assertFalse(state.canAuthenticate)
    }

    @Test
    fun testGetKeyStateAvailable() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.hardwareDetected = true
        fakeLocalDataSource.enrolledBiometrics = true
        fakeLocalDataSource.deviceBiometricsChanged = false
        fakeLocalDataSource.keyReady = true
        val state = repository.getKeyState()

        assertEquals(BiometricStatus.Available, state.status)
        assertTrue(state.isKeyReady)
        assertFalse(state.hasChangedFingerprints)
        assertTrue(state.canAuthenticate)
    }

    @Test
    fun testCheckKeyReadyAndDelegation() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.keyReady = false

        val result = repository.checkKeyReady(true)
        assertTrue(result is Result.Success)
        assertEquals(1, fakeLocalDataSource.checkKeyReadyCallCount)
        assertTrue(repository.isKeyReady())

        fakeLocalDataSource.deviceBiometricsChanged = true
        assertTrue(repository.hasDeviceBiometricsChanged())
    }

    @Test
    fun testDeleteInvalidKey() = runTest {
        fakeLocalDataSource.supported = true
        fakeLocalDataSource.keyReady = true

        val result = repository.deleteInvalidKey()
        assertTrue(result is Result.Success)
        assertEquals(1, fakeLocalDataSource.deleteInvalidKeyCallCount)
        assertFalse(repository.isKeyReady())
    }

    @Test
    fun testStranglerHook() {
        val repo = FingerprintController.getBiometricsRepository()
        assertNotNull(repo)
    }

    private class FakeBiometricsLocalDataSource : BiometricsLocalDataSource() {
        var supported = true
        var hardwareDetected = true
        var enrolledBiometrics = true
        var keyReady = false
        var deviceBiometricsChanged = false
        var checkKeyReadyCallCount = 0
        var deleteInvalidKeyCallCount = 0

        override fun isSupported(): Boolean = supported
        override fun isHardwareDetected(): Boolean = hardwareDetected
        override fun hasEnrolledBiometrics(): Boolean = enrolledBiometrics
        override fun isKeyReady(): Boolean = keyReady
        override fun hasDeviceBiometricsChanged(): Boolean = deviceBiometricsChanged

        override fun checkKeyReady(notifyCheckFingerprint: Boolean) {
            checkKeyReadyCallCount++
            keyReady = true
        }

        override fun deleteInvalidKey() {
            deleteInvalidKeyCallCount++
            keyReady = false
            deviceBiometricsChanged = false
        }
    }
}
