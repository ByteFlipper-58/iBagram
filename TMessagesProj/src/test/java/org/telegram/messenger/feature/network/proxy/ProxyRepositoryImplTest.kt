package org.telegram.messenger.feature.network.proxy

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
import org.telegram.messenger.ProxyRotationController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyLocalDataSource
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyRemoteDataSource
import org.telegram.messenger.feature.network.proxy.data.repository.ProxyRepositoryImpl
import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProxyRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLocalDataSource: FakeProxyLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeProxyRemoteDataSource
    private lateinit var repository: ProxyRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLocalDataSource = FakeProxyLocalDataSource()
        fakeRemoteDataSource = FakeProxyRemoteDataSource()
        repository = ProxyRepositoryImpl(
            account = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = testDispatcher,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGetProxySettings() = runTest {
        val proxy = SharedConfig.ProxyInfo("127.0.0.1", 1080, "u", "p", "")
        fakeLocalDataSource.fakeProxyList.add(proxy)
        fakeLocalDataSource.fakeCurrentProxy = proxy
        fakeLocalDataSource.fakeProxyEnabled = true
        fakeLocalDataSource.fakeRotationEnabled = true
        fakeLocalDataSource.fakeRotationTimeoutIndex = 2 // 15 seconds

        val settings = repository.getProxySettings()
        assertTrue(settings.isEnabled)
        assertNotNull(settings.currentProxy)
        assertEquals("127.0.0.1", settings.currentProxy?.address)
        assertEquals(1080, settings.currentProxy?.port)
        assertTrue(settings.isRotationEnabled)
        assertEquals(15, settings.rotationTimeoutMinutes)
        assertEquals(1, settings.proxyList.size)
    }

    @Test
    fun testAddProxySuccess() = runTest {
        val result = repository.addProxy("1.2.3.4", 9050, "user", "pass", "")
        assertTrue(result is Result.Success)
        val model = (result as Result.Success).data
        assertEquals("1.2.3.4", model.address)
        assertEquals(9050, model.port)
        assertEquals(1, fakeLocalDataSource.fakeProxyList.size)
        assertTrue(fakeLocalDataSource.proxySettingsChangedNotified)
    }

    @Test
    fun testAddProxyInvalidInput() = runTest {
        val resultEmpty = repository.addProxy("", 8080)
        assertTrue(resultEmpty is Result.Failure)

        val resultBadPort = repository.addProxy("example.com", 70000)
        assertTrue(resultBadPort is Result.Failure)
    }

    @Test
    fun testDeleteProxy() = runTest {
        val proxyInfo = SharedConfig.ProxyInfo("1.1.1.1", 8080, "", "", "")
        fakeLocalDataSource.fakeProxyList.add(proxyInfo)

        val domainProxy = ProxyModel(address = "1.1.1.1", port = 8080)
        val result = repository.deleteProxy(domainProxy)
        assertTrue(result is Result.Success)
        assertEquals(0, fakeLocalDataSource.fakeProxyList.size)
        assertTrue(fakeLocalDataSource.proxySettingsChangedNotified)
    }

    @Test
    fun testEnableProxy() = runTest {
        val domainProxy = ProxyModel(address = "10.0.0.1", port = 443, secret = "secret123")
        val result = repository.enableProxy(domainProxy)
        assertTrue(result is Result.Success)

        assertTrue(fakeLocalDataSource.fakeProxyEnabled)
        assertEquals("10.0.0.1", fakeLocalDataSource.fakeCurrentProxy?.address)
        assertEquals(true, fakeRemoteDataSource.lastAppliedEnabled)
        assertEquals("10.0.0.1", fakeRemoteDataSource.lastAppliedAddress)
        assertTrue(fakeLocalDataSource.proxySettingsChangedNotified)
    }

    @Test
    fun testDisableProxy() = runTest {
        fakeLocalDataSource.fakeProxyEnabled = true
        fakeLocalDataSource.fakeCurrentProxy = SharedConfig.ProxyInfo("10.0.0.1", 443, "", "", "")

        val result = repository.disableProxy()
        assertTrue(result is Result.Success)
        assertFalse(fakeLocalDataSource.fakeProxyEnabled)
        assertEquals(false, fakeRemoteDataSource.lastAppliedEnabled)
        assertTrue(fakeLocalDataSource.proxySettingsChangedNotified)
    }

    @Test
    fun testToggleProxyRotation() = runTest {
        val result = repository.toggleProxyRotation(enabled = true, timeoutMinutes = 30)
        assertTrue(result is Result.Success)
        assertTrue(fakeLocalDataSource.fakeRotationEnabled)
        // 30 is at index 3 in ROTATION_TIMEOUTS (5, 10, 15, 30, 60)
        assertEquals(3, fakeLocalDataSource.fakeRotationTimeoutIndex)
        assertTrue(fakeLocalDataSource.proxySettingsChangedNotified)
    }

    @Test
    fun testCheckProxyPingSuccess() = runTest {
        val proxyInfo = SharedConfig.ProxyInfo("proxy.fast.org", 443, "", "", "")
        fakeLocalDataSource.fakeProxyList.add(proxyInfo)
        fakeRemoteDataSource.pingResultToReturn = Result.Success(45L)

        val domainProxy = ProxyModel(address = "proxy.fast.org", port = 443)
        val result = repository.checkProxyPing(domainProxy)
        assertTrue(result is Result.Success)
        assertEquals(45L, (result as Result.Success).data)
        assertTrue(proxyInfo.available)
        assertEquals(45L, proxyInfo.ping)
        assertTrue(fakeLocalDataSource.proxyCheckDoneNotified)
    }

    @Test
    fun testCheckProxyPingFailure() = runTest {
        val proxyInfo = SharedConfig.ProxyInfo("proxy.dead.org", 443, "", "", "")
        fakeLocalDataSource.fakeProxyList.add(proxyInfo)
        fakeRemoteDataSource.pingResultToReturn = Result.Failure(org.telegram.messenger.core.result.AppError.Network("Unreachable"))

        val domainProxy = ProxyModel(address = "proxy.dead.org", port = 443)
        val result = repository.checkProxyPing(domainProxy)
        assertTrue(result is Result.Failure)
        assertFalse(proxyInfo.available)
        assertEquals(0L, proxyInfo.ping)
        assertTrue(fakeLocalDataSource.proxyCheckDoneNotified)
    }

    @Test
    fun testStranglerHook() {
        val repo = ProxyRotationController.getProxyRepository(0)
        assertNotNull(repo)
    }

    // Fakes
    private class FakeProxyLocalDataSource : ProxyLocalDataSource(0) {
        val fakeProxyList = mutableListOf<SharedConfig.ProxyInfo>()
        var fakeCurrentProxy: SharedConfig.ProxyInfo? = null
        var fakeProxyEnabled = false
        var fakeRotationEnabled = false
        var fakeRotationTimeoutIndex = 1
        var fakeCallsWithProxy = false
        var proxySettingsChangedNotified = false
        var proxyCheckDoneNotified = false

        override fun getProxyList(): List<SharedConfig.ProxyInfo> = fakeProxyList
        override fun getCurrentProxy(): SharedConfig.ProxyInfo? = fakeCurrentProxy
        override fun isProxyEnabled(): Boolean = fakeProxyEnabled
        override fun isProxyRotationEnabled(): Boolean = fakeRotationEnabled
        override fun getProxyRotationTimeoutIndex(): Int = fakeRotationTimeoutIndex
        override fun getUseCallsWithProxy(): Boolean = fakeCallsWithProxy

        override suspend fun addProxy(info: SharedConfig.ProxyInfo): SharedConfig.ProxyInfo {
            fakeProxyList.removeAll { it.address == info.address && it.port == info.port && it.secret == info.secret }
            fakeProxyList.add(info)
            return info
        }

        override suspend fun deleteProxy(info: SharedConfig.ProxyInfo) {
            fakeProxyList.removeAll { it.address == info.address && it.port == info.port && it.secret == info.secret }
            if (fakeCurrentProxy?.address == info.address && fakeCurrentProxy?.port == info.port) {
                fakeCurrentProxy = null
            }
        }

        override suspend fun saveProxyPreferences(enabled: Boolean, proxyInfo: SharedConfig.ProxyInfo?) {
            fakeProxyEnabled = enabled
            fakeCurrentProxy = proxyInfo
        }

        override suspend fun saveRotationSettings(enabled: Boolean, timeoutIndex: Int) {
            fakeRotationEnabled = enabled
            fakeRotationTimeoutIndex = timeoutIndex
        }

        override fun notifyProxySettingsChanged() {
            proxySettingsChangedNotified = true
        }

        override fun notifyProxyCheckDone(info: SharedConfig.ProxyInfo) {
            proxyCheckDoneNotified = true
        }
    }

    private class FakeProxyRemoteDataSource : ProxyRemoteDataSource(0) {
        var pingResultToReturn: Result<Long> = Result.Success(50L)
        var lastAppliedEnabled: Boolean? = null
        var lastAppliedAddress: String? = null

        override suspend fun checkProxyPing(
            address: String,
            port: Int,
            username: String,
            password: String,
            secret: String
        ): Result<Long> {
            return pingResultToReturn
        }

        override fun applyProxySettings(
            enabled: Boolean,
            address: String,
            port: Int,
            username: String,
            password: String,
            secret: String
        ) {
            lastAppliedEnabled = enabled
            lastAppliedAddress = address
        }
    }
}
