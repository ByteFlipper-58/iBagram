package org.telegram.messenger.feature.biometrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.biometrics.data.mapper.BiometricMapper
import org.telegram.messenger.feature.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.biometrics.domain.model.BiometricStatus
import org.telegram.messenger.feature.biometrics.domain.repository.BiometricsRepository
import org.telegram.messenger.feature.biometrics.domain.usecase.CheckBiometricKeyReadyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.DeleteInvalidBiometricKeyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.GetBiometricKeyStateUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.HasDeviceBiometricsChangedUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.IsBiometricKeyReadyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.ObserveBiometricKeyStateUseCase
import org.telegram.messenger.feature.biometrics.presentation.BiometricsEvent
import org.telegram.messenger.feature.biometrics.presentation.BiometricsUiState
import org.telegram.messenger.feature.biometrics.presentation.BiometricsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBiometricsRepository : BiometricsRepository {
        var keyReady = false
        var changedFingerprints = false
        var status: BiometricStatus = BiometricStatus.Available
        var shouldFail = false
        var checkCallCount = 0
        var deleteCallCount = 0

        private val stateFlow = MutableStateFlow(
            BiometricKeyStateModel(
                isKeyReady = keyReady,
                hasChangedFingerprints = changedFingerprints,
                status = status
            )
        )

        fun updateState(newKeyReady: Boolean, newChanged: Boolean, newStatus: BiometricStatus) {
            keyReady = newKeyReady
            changedFingerprints = newChanged
            status = newStatus
            stateFlow.value = BiometricKeyStateModel(
                isKeyReady = keyReady,
                hasChangedFingerprints = changedFingerprints,
                status = status
            )
        }

        override fun observeKeyState(): Flow<BiometricKeyStateModel> = stateFlow.asStateFlow()

        override suspend fun getKeyState(): BiometricKeyStateModel {
            return BiometricKeyStateModel(
                isKeyReady = keyReady,
                hasChangedFingerprints = changedFingerprints,
                status = status
            )
        }

        override suspend fun checkKeyReady(notifyCheckFingerprint: Boolean): Result<Boolean> {
            checkCallCount++
            if (shouldFail) {
                return Result.failure("Check key failure simulated")
            }
            keyReady = true
            stateFlow.value = BiometricKeyStateModel(keyReady, changedFingerprints, status)
            return Result.Success(true)
        }

        override suspend fun deleteInvalidKey(): Result<Unit> {
            deleteCallCount++
            if (shouldFail) {
                return Result.failure("Delete key failure simulated")
            }
            keyReady = false
            changedFingerprints = false
            stateFlow.value = BiometricKeyStateModel(keyReady, changedFingerprints, status)
            return Result.Success(Unit)
        }

        override fun isKeyReady(): Boolean = keyReady

        override fun hasDeviceBiometricsChanged(): Boolean = changedFingerprints
    }

    @Test
    fun testBiometricStatusProperties() {
        assertTrue(BiometricStatus.Available.isAvailable)
        assertFalse(BiometricStatus.HardwareUnavailable.isAvailable)
        assertFalse(BiometricStatus.NoEnrolledBiometrics.isAvailable)
        assertFalse(BiometricStatus.KeyPermanentlyInvalidated.isAvailable)
        assertFalse(BiometricStatus.NotSupported.isAvailable)
    }

    @Test
    fun testBiometricKeyStateModelCanAuthenticate() {
        val readyState = BiometricKeyStateModel(
            isKeyReady = true,
            hasChangedFingerprints = false,
            status = BiometricStatus.Available
        )
        assertTrue(readyState.canAuthenticate)

        val notReadyState = readyState.copy(isKeyReady = false)
        assertFalse(notReadyState.canAuthenticate)

        val changedState = readyState.copy(hasChangedFingerprints = true)
        assertFalse(changedState.canAuthenticate)

        val hardwareUnavailState = readyState.copy(status = BiometricStatus.HardwareUnavailable)
        assertFalse(hardwareUnavailState.canAuthenticate)

        val noEnrolledState = readyState.copy(status = BiometricStatus.NoEnrolledBiometrics)
        assertFalse(noEnrolledState.canAuthenticate)

        val invalidatedState = readyState.copy(status = BiometricStatus.KeyPermanentlyInvalidated)
        assertFalse(invalidatedState.canAuthenticate)
    }

    @Test
    fun testBiometricMapper() {
        assertEquals(
            BiometricStatus.NotSupported,
            BiometricMapper.mapStatus(
                isSupported = false,
                isHardwareDetected = true,
                hasEnrolledBiometrics = true,
                isPermanentlyInvalidated = false
            )
        )

        assertEquals(
            BiometricStatus.HardwareUnavailable,
            BiometricMapper.mapStatus(
                isSupported = true,
                isHardwareDetected = false,
                hasEnrolledBiometrics = true,
                isPermanentlyInvalidated = false
            )
        )

        assertEquals(
            BiometricStatus.NoEnrolledBiometrics,
            BiometricMapper.mapStatus(
                isSupported = true,
                isHardwareDetected = true,
                hasEnrolledBiometrics = false,
                isPermanentlyInvalidated = false
            )
        )

        assertEquals(
            BiometricStatus.KeyPermanentlyInvalidated,
            BiometricMapper.mapStatus(
                isSupported = true,
                isHardwareDetected = true,
                hasEnrolledBiometrics = true,
                isPermanentlyInvalidated = true
            )
        )

        assertEquals(
            BiometricStatus.Available,
            BiometricMapper.mapStatus(
                isSupported = true,
                isHardwareDetected = true,
                hasEnrolledBiometrics = true,
                isPermanentlyInvalidated = false
            )
        )

        val mappedModel = BiometricMapper.toKeyStateModel(
            isKeyReady = true,
            hasChangedFingerprints = false,
            status = BiometricStatus.Available
        )
        assertTrue(mappedModel.isKeyReady)
        assertFalse(mappedModel.hasChangedFingerprints)
        assertEquals(BiometricStatus.Available, mappedModel.status)
        assertTrue(mappedModel.canAuthenticate)
    }

    @Test
    fun testUseCases() = runTest {
        val repo = FakeBiometricsRepository()
        val observeUseCase = ObserveBiometricKeyStateUseCase(repo)
        val getUseCase = GetBiometricKeyStateUseCase(repo)
        val checkUseCase = CheckBiometricKeyReadyUseCase(repo)
        val deleteUseCase = DeleteInvalidBiometricKeyUseCase(repo)
        val isReadyUseCase = IsBiometricKeyReadyUseCase(repo)
        val hasChangedUseCase = HasDeviceBiometricsChangedUseCase(repo)

        val initial = getUseCase()
        assertFalse(initial.isKeyReady)
        assertFalse(isReadyUseCase())
        assertFalse(hasChangedUseCase())

        val checkRes = checkUseCase(true)
        assertTrue(checkRes.isSuccess)
        assertTrue(isReadyUseCase())
        assertEquals(1, repo.checkCallCount)

        val current = getUseCase()
        assertTrue(current.isKeyReady)
        assertTrue(current.canAuthenticate)

        val deleteRes = deleteUseCase()
        assertTrue(deleteRes.isSuccess)
        assertFalse(isReadyUseCase())
        assertEquals(1, repo.deleteCallCount)

        repo.shouldFail = true
        val failedCheck = checkUseCase()
        assertTrue(failedCheck.isFailure)

        val failedDelete = deleteUseCase()
        assertTrue(failedDelete.isFailure)

        repo.shouldFail = false
        repo.updateState(newKeyReady = true, newChanged = true, newStatus = BiometricStatus.KeyPermanentlyInvalidated)
        val observed = observeUseCase().first()
        assertTrue(observed.isKeyReady)
        assertTrue(observed.hasChangedFingerprints)
        assertEquals(BiometricStatus.KeyPermanentlyInvalidated, observed.status)
        assertFalse(observed.canAuthenticate)
    }

    @Test
    fun testBiometricsViewModel() = runTest {
        val repo = FakeBiometricsRepository()
        val observeUseCase = ObserveBiometricKeyStateUseCase(repo)
        val getUseCase = GetBiometricKeyStateUseCase(repo)
        val checkUseCase = CheckBiometricKeyReadyUseCase(repo)
        val deleteUseCase = DeleteInvalidBiometricKeyUseCase(repo)
        val isReadyUseCase = IsBiometricKeyReadyUseCase(repo)
        val hasChangedUseCase = HasDeviceBiometricsChangedUseCase(repo)

        val vm = BiometricsViewModel(
            observeBiometricKeyStateUseCase = observeUseCase,
            getBiometricKeyStateUseCase = getUseCase,
            checkBiometricKeyReadyUseCase = checkUseCase,
            deleteInvalidBiometricKeyUseCase = deleteUseCase,
            isBiometricKeyReadyUseCase = isReadyUseCase,
            hasDeviceBiometricsChangedUseCase = hasChangedUseCase
        )

        advanceUntilIdle()
        val state1 = vm.uiState.value
        assertTrue(state1 is BiometricsUiState.Ready)
        val readyState1 = state1 as BiometricsUiState.Ready
        assertFalse(readyState1.keyState.isKeyReady)
        assertFalse(vm.isKeyReady())
        assertFalse(vm.hasDeviceBiometricsChanged())

        // Check key ready
        vm.onEvent(BiometricsEvent.CheckKeyReady(true))
        advanceUntilIdle()
        val state2 = vm.uiState.value as BiometricsUiState.Ready
        assertTrue(state2.keyState.isKeyReady)
        assertTrue(vm.isKeyReady())

        // Delete invalid key
        vm.onEvent(BiometricsEvent.DeleteInvalidKey)
        advanceUntilIdle()
        val state3 = vm.uiState.value as BiometricsUiState.Ready
        assertFalse(state3.keyState.isKeyReady)
        assertFalse(vm.isKeyReady())

        // Refresh state
        repo.updateState(newKeyReady = true, newChanged = false, newStatus = BiometricStatus.Available)
        vm.onEvent(BiometricsEvent.RefreshState)
        advanceUntilIdle()
        val state4 = vm.uiState.value as BiometricsUiState.Ready
        assertTrue(state4.keyState.isKeyReady)
        assertTrue(state4.keyState.canAuthenticate)

        // Clear error
        vm.onEvent(BiometricsEvent.ClearError)
        advanceUntilIdle()
        assertNull((vm.uiState.value as BiometricsUiState.Ready).errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.biometricsRepository)
        assertNotNull(container.observeBiometricKeyStateUseCase)
        assertNotNull(container.getBiometricKeyStateUseCase)
        assertNotNull(container.checkBiometricKeyReadyUseCase)
        assertNotNull(container.deleteInvalidBiometricKeyUseCase)
        assertNotNull(container.isBiometricKeyReadyUseCase)
        assertNotNull(container.hasDeviceBiometricsChangedUseCase)
        assertNotNull(container.biometricsViewModel)

        val fakeRepo = FakeBiometricsRepository()
        container.biometricsRepository = fakeRepo
        assertEquals(fakeRepo, container.biometricsRepository)

        val newVm = container.createBiometricsViewModel()
        assertNotNull(newVm)
    }
}
