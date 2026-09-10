package org.telegram.messenger.feature.network.push

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.network.push.data.mapper.PushMapper
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository
import org.telegram.messenger.feature.network.push.domain.usecase.GetPushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.IsPushAvailableUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ObservePushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RegisterPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RequestPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ResetPushTokenUseCase
import org.telegram.messenger.feature.network.push.presentation.PushEvent
import org.telegram.messenger.feature.network.push.presentation.PushViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PushDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakePushRepository : PushRepository {
        var token: String? = "fake_fcm_token_123"
        var serviceType: PushServiceType = PushServiceType.FIREBASE
        var status: String? = null
        var hasServices: Boolean = true
        var isRegistered: Boolean = true
        var registeredAccountsCount: Int = 1
        var providerTitle: String = "Google Play Services"

        var shouldFailRequestToken: Boolean = false
        var shouldFailRegister: Boolean = false
        var shouldFailReset: Boolean = false

        private val stateFlow = MutableStateFlow(computeStatus())

        private fun computeStatus() = PushStatusModel(
            token = token,
            serviceType = serviceType,
            status = status,
            hasServices = hasServices,
            isRegisteredForCurrentAccount = isRegistered,
            registeredAccountsCount = registeredAccountsCount,
            providerTitle = providerTitle
        )

        private fun notifyChange() {
            stateFlow.value = computeStatus()
        }

        override fun observePushStatus(): Flow<PushStatusModel> = stateFlow.asStateFlow()

        override fun getPushStatus(): PushStatusModel = computeStatus()

        override fun isPushServiceAvailable(): Boolean = hasServices

        override suspend fun requestPushToken(): Result<PushRegistrationResult> {
            return if (shouldFailRequestToken) {
                Result.success(PushRegistrationResult.Failure("FCM initialization failed"))
            } else {
                token = "new_fcm_token_456"
                notifyChange()
                Result.success(PushRegistrationResult.Success(token, serviceType))
            }
        }

        override suspend fun registerPushToken(
            serviceType: PushServiceType,
            token: String?
        ): Result<Unit> {
            return if (shouldFailRegister) {
                Result.failure(RuntimeException("Network error registering push token"))
            } else {
                this.serviceType = serviceType
                this.token = token
                this.isRegistered = token != null
                notifyChange()
                Result.success(Unit)
            }
        }

        override suspend fun resetPushToken(): Result<Unit> {
            return if (shouldFailReset) {
                Result.failure(RuntimeException("Reset error"))
            } else {
                this.token = null
                this.isRegistered = false
                notifyChange()
                Result.success(Unit)
            }
        }
    }

    @Test
    fun testPushServiceTypeMapping() {
        assertEquals(PushServiceType.FIREBASE, PushServiceType.fromTypeId(2))
        assertEquals(PushServiceType.HUAWEI, PushServiceType.fromTypeId(13))
        assertEquals(PushServiceType.UNKNOWN, PushServiceType.fromTypeId(0))
        assertEquals(PushServiceType.UNKNOWN, PushServiceType.fromTypeId(99))

        assertEquals(2, PushServiceType.FIREBASE.typeId)
        assertEquals(13, PushServiceType.HUAWEI.typeId)
        assertEquals(0, PushServiceType.UNKNOWN.typeId)
    }

    @Test
    fun testPushStatusModelValidation() {
        val validStatus = PushStatusModel(
            token = "token_abc",
            serviceType = PushServiceType.FIREBASE,
            status = null,
            hasServices = true,
            isRegisteredForCurrentAccount = true,
            registeredAccountsCount = 1,
            providerTitle = "Google Play Services"
        )
        assertTrue(validStatus.isTokenValid)

        val invalidStatus1 = validStatus.copy(token = null)
        assertFalse(invalidStatus1.isTokenValid)

        val invalidStatus2 = validStatus.copy(token = "")
        assertFalse(invalidStatus2.isTokenValid)

        val failedStatus = validStatus.copy(status = "__FIREBASE_FAILED__")
        assertFalse(failedStatus.isTokenValid)
    }

    @Test
    fun testPushMapper() {
        assertEquals(PushServiceType.FIREBASE, PushMapper.toPushServiceType(PushListenerController.PUSH_TYPE_FIREBASE))
        assertEquals(PushServiceType.HUAWEI, PushMapper.toPushServiceType(PushListenerController.PUSH_TYPE_HUAWEI))
        assertEquals(PushServiceType.UNKNOWN, PushMapper.toPushServiceType(999))

        assertEquals(PushListenerController.PUSH_TYPE_FIREBASE, PushMapper.toLegacyPushType(PushServiceType.FIREBASE))
        assertEquals(PushListenerController.PUSH_TYPE_HUAWEI, PushMapper.toLegacyPushType(PushServiceType.HUAWEI))
        assertEquals(PushListenerController.PUSH_TYPE_FIREBASE, PushMapper.toLegacyPushType(PushServiceType.UNKNOWN))

        val status = PushMapper.mapToPushStatus(
            token = "fcm_xyz",
            legacyPushType = PushListenerController.PUSH_TYPE_FIREBASE,
            status = null,
            hasServices = true,
            isRegisteredForAccount = true,
            registeredAccountsCount = 2,
            providerTitle = "Google Play Services"
        )
        assertEquals("fcm_xyz", status.token)
        assertEquals(PushServiceType.FIREBASE, status.serviceType)
        assertTrue(status.hasServices)
        assertTrue(status.isRegisteredForCurrentAccount)
        assertEquals(2, status.registeredAccountsCount)
        assertEquals("Google Play Services", status.providerTitle)
    }

    @Test
    fun testPushUseCases() = runTest {
        val repo = FakePushRepository()
        val getStatusUseCase = GetPushStatusUseCase(repo)
        val isAvailableUseCase = IsPushAvailableUseCase(repo)
        val requestTokenUseCase = RequestPushTokenUseCase(repo)
        val registerTokenUseCase = RegisterPushTokenUseCase(repo)
        val resetTokenUseCase = ResetPushTokenUseCase(repo)

        val initialStatus = getStatusUseCase()
        assertEquals("fake_fcm_token_123", initialStatus.token)
        assertTrue(isAvailableUseCase())

        // Request push token
        val reqResult = requestTokenUseCase()
        assertTrue(reqResult.isSuccess)
        val regRes = reqResult.getOrNull()
        assertTrue(regRes is PushRegistrationResult.Success)
        assertEquals("new_fcm_token_456", (regRes as PushRegistrationResult.Success).token)
        assertEquals("new_fcm_token_456", getStatusUseCase().token)

        // Register push token
        val regCallResult = registerTokenUseCase(PushServiceType.HUAWEI, "huawei_token_789")
        assertTrue(regCallResult.isSuccess)
        assertEquals("huawei_token_789", getStatusUseCase().token)
        assertEquals(PushServiceType.HUAWEI, getStatusUseCase().serviceType)

        // Reset push token
        val resetResult = resetTokenUseCase()
        assertTrue(resetResult.isSuccess)
        assertNull(getStatusUseCase().token)
        assertFalse(getStatusUseCase().isRegisteredForCurrentAccount)

        // Test failures
        repo.shouldFailRequestToken = true
        val failedReq = requestTokenUseCase()
        assertTrue(failedReq.isSuccess)
        assertTrue(failedReq.getOrNull() is PushRegistrationResult.Failure)

        repo.shouldFailRegister = true
        val failedReg = registerTokenUseCase(PushServiceType.FIREBASE, "fail_token")
        assertTrue(failedReg.isFailure)

        repo.shouldFailReset = true
        val failedReset = resetTokenUseCase()
        assertTrue(failedReset.isFailure)
    }

    @Test
    fun testPushViewModel() = runTest {
        val repo = FakePushRepository()
        val vm = PushViewModel(
            observePushStatusUseCase = ObservePushStatusUseCase(repo),
            getPushStatusUseCase = GetPushStatusUseCase(repo),
            isPushAvailableUseCase = IsPushAvailableUseCase(repo),
            requestPushTokenUseCase = RequestPushTokenUseCase(repo),
            registerPushTokenUseCase = RegisterPushTokenUseCase(repo),
            resetPushTokenUseCase = ResetPushTokenUseCase(repo)
        )

        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("fake_fcm_token_123", state.status?.token)
        assertTrue(state.isAvailable)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        // Request token event
        vm.onEvent(PushEvent.RequestPushToken)
        advanceUntilIdle()
        assertEquals("new_fcm_token_456", vm.uiState.value.status?.token)
        assertEquals("Push token requested successfully", vm.uiState.value.infoMessage)

        // Dismiss info
        vm.onEvent(PushEvent.DismissInfo)
        assertNull(vm.uiState.value.infoMessage)

        // Register token event
        vm.onEvent(PushEvent.RegisterPushToken(PushServiceType.FIREBASE, "registered_token_888"))
        advanceUntilIdle()
        assertEquals("registered_token_888", vm.uiState.value.status?.token)
        assertEquals("Token registered on server", vm.uiState.value.infoMessage)

        // Failure handling in register
        repo.shouldFailRegister = true
        vm.onEvent(PushEvent.RegisterPushToken(PushServiceType.FIREBASE, "another_token"))
        advanceUntilIdle()
        assertEquals("Network error registering push token", vm.uiState.value.errorMessage)

        // Dismiss error
        vm.onEvent(PushEvent.DismissError)
        assertNull(vm.uiState.value.errorMessage)

        // Reset token
        repo.shouldFailReset = false
        vm.onEvent(PushEvent.ResetPushToken)
        advanceUntilIdle()
        assertNull(vm.uiState.value.status?.token)
        assertEquals("Push token reset requested", vm.uiState.value.infoMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        assertNotNull(container.pushRepository)
        assertNotNull(container.observePushStatusUseCase)
        assertNotNull(container.getPushStatusUseCase)
        assertNotNull(container.isPushAvailableUseCase)
        assertNotNull(container.requestPushTokenUseCase)
        assertNotNull(container.registerPushTokenUseCase)
        assertNotNull(container.resetPushTokenUseCase)

        val fakeRepo = FakePushRepository()
        container.pushRepository = fakeRepo
        assertEquals(fakeRepo, container.pushRepository)
        assertEquals(fakeRepo.getPushStatus().token, container.getPushStatusUseCase().token)

        val vm = container.createPushViewModel()
        assertNotNull(vm)
        assertEquals("fake_fcm_token_123", vm.uiState.value.status?.token)
    }
}
