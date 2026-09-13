package org.telegram.messenger.feature.network.push

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.feature.network.push.data.datasource.PushLocalDataSource
import org.telegram.messenger.feature.network.push.data.datasource.PushRemoteDataSource
import org.telegram.messenger.feature.network.push.data.repository.PushRepositoryImpl
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType

@OptIn(ExperimentalCoroutinesApi::class)
class PushRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    private class FakePushRemoteDataSource(account: Int) : PushRemoteDataSource(account) {
        var lastSentType: PushServiceType? = null
        var lastSentToken: String? = null
        var lastResetType: Int? = null
        var tokenRequested = false

        override suspend fun requestPushToken(
            provider: PushListenerController.IPushListenerServiceProvider?,
            currentToken: String?
        ): Result<PushRegistrationResult> {
            tokenRequested = true
            return if (provider != null) {
                Result.success(PushRegistrationResult.Success(currentToken ?: "fake_token_123", PushServiceType.FIREBASE))
            } else {
                Result.success(PushRegistrationResult.Failure("Push provider not available"))
            }
        }

        override suspend fun sendRegistrationToServer(
            serviceType: PushServiceType,
            token: String?
        ): Result<Unit> {
            lastSentType = serviceType
            lastSentToken = token
            return Result.success(Unit)
        }

        override suspend fun resetRegistration(
            legacyType: Int,
            provider: PushListenerController.IPushListenerServiceProvider?
        ): Result<Unit> {
            lastResetType = legacyType
            return Result.success(Unit)
        }
    }

    @Test
    fun testGetPushStatusReturnsValidModel() = runTest {
        val local = PushLocalDataSource(0)
        val remote = FakePushRemoteDataSource(0)
        val repo = PushRepositoryImpl(0, local, remote, testDispatcher)

        local.setPushToken("test_token_abc")
        local.setPushType(PushListenerController.PUSH_TYPE_FIREBASE)
        local.setPushStatusString("CONNECTED")
        local.setHasServices(true)
        local.setProviderTitle("FCM")
        local.setRegisteredForAccount(0, true)

        val status = repo.getPushStatus()
        assertNotNull(status)
        assertEquals("test_token_abc", status.token)
        assertEquals(PushServiceType.FIREBASE, status.serviceType)
        assertEquals("CONNECTED", status.status)
        assertTrue(status.hasServices)
        assertTrue(status.isRegisteredForCurrentAccount)
        assertEquals("FCM", status.providerTitle)
        assertTrue(status.isTokenValid)
        assertTrue(repo.isPushServiceAvailable())
    }

    @Test
    fun testObservePushStatusEmitsCurrent() = runTest {
        val local = PushLocalDataSource(0)
        local.setPushToken("flow_token")
        val remote = FakePushRemoteDataSource(0)
        val repo = PushRepositoryImpl(0, local, remote, testDispatcher)

        val emitted = repo.observePushStatus().first()
        assertEquals("flow_token", emitted.token)
    }

    @Test
    fun testRequestPushTokenWithoutProviderFails() = runTest {
        val local = PushLocalDataSource(0)
        val remote = FakePushRemoteDataSource(0)
        val repo = PushRepositoryImpl(0, local, remote, testDispatcher)

        val result = repo.requestPushToken()
        assertTrue(result.isSuccess)
        val reg = result.getOrNull()
        assertTrue(reg is PushRegistrationResult.Failure)
        assertTrue(remote.tokenRequested)
    }

    @Test
    fun testRegisterPushTokenDelegatesToRemote() = runTest {
        val local = PushLocalDataSource(0)
        val remote = FakePushRemoteDataSource(0)
        val repo = PushRepositoryImpl(0, local, remote, testDispatcher)

        val res = repo.registerPushToken(PushServiceType.HUAWEI, "huawei_token_xyz")
        assertTrue(res.isSuccess)
        assertEquals(PushServiceType.HUAWEI, remote.lastSentType)
        assertEquals("huawei_token_xyz", remote.lastSentToken)
    }

    @Test
    fun testResetPushTokenDelegatesToRemote() = runTest {
        val local = PushLocalDataSource(0)
        val remote = FakePushRemoteDataSource(0)
        val repo = PushRepositoryImpl(0, local, remote, testDispatcher)

        local.setPushType(PushListenerController.PUSH_TYPE_FIREBASE)

        val res = repo.resetPushToken()
        assertTrue(res.isSuccess)
        assertEquals(PushListenerController.PUSH_TYPE_FIREBASE, remote.lastResetType)
    }

    @Test
    fun testContainerWiringDefaultsToImpl() {
        val container = AccountFeatureContainer.get(0)
        val repo = container.pushRepository
        assertTrue(repo is PushRepositoryImpl)

        val created = container.network.createPushRepository()
        assertTrue(created is PushRepositoryImpl)
    }
}
