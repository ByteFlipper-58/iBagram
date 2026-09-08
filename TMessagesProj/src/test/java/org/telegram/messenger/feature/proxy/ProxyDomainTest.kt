package org.telegram.messenger.feature.proxy

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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.proxy.domain.model.ProxyType
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository
import org.telegram.messenger.feature.proxy.domain.usecase.AddProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.CheckProxyPingUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DeleteProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DisableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.EnableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.GetProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ObserveProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ToggleProxyRotationUseCase
import org.telegram.messenger.feature.proxy.presentation.ProxyEvent
import org.telegram.messenger.feature.proxy.presentation.ProxyUiState
import org.telegram.messenger.feature.proxy.presentation.ProxyViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class ProxyDomainTest {

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
    fun testProxyModelPropertiesAndType() {
        val socksProxy = ProxyModel(
            address = "192.168.1.1",
            port = 1080,
            username = "user",
            password = "pwd"
        )
        assertEquals(ProxyType.SOCKS5, socksProxy.type)
        assertTrue(socksProxy.link.startsWith("https://t.me/socks?"))
        assertTrue(socksProxy.link.contains("server=192.168.1.1"))
        assertTrue(socksProxy.link.contains("port=1080"))
        assertTrue(socksProxy.link.contains("user=user"))
        assertTrue(socksProxy.link.contains("pass=pwd"))

        val mtprotoProxy = ProxyModel(
            address = "proxy.example.com",
            port = 443,
            secret = "ee1234567890abcdef"
        )
        assertEquals(ProxyType.MTPROTO, mtprotoProxy.type)
        assertTrue(mtprotoProxy.link.startsWith("https://t.me/proxy?"))
        assertTrue(mtprotoProxy.link.contains("server=proxy.example.com"))
        assertTrue(mtprotoProxy.link.contains("port=443"))
        assertTrue(mtprotoProxy.link.contains("secret=ee1234567890abcdef"))
    }

    @Test
    fun testProxyUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakeProxyRepository()

        val observeUseCase = ObserveProxySettingsUseCase(fakeRepo)
        val getUseCase = GetProxySettingsUseCase(fakeRepo)
        val addUseCase = AddProxyUseCase(fakeRepo)
        val deleteUseCase = DeleteProxyUseCase(fakeRepo)
        val enableUseCase = EnableProxyUseCase(fakeRepo)
        val disableUseCase = DisableProxyUseCase(fakeRepo)
        val toggleRotationUseCase = ToggleProxyRotationUseCase(fakeRepo)
        val checkPingUseCase = CheckProxyPingUseCase(fakeRepo)

        // Initial settings
        val initial = getUseCase()
        assertFalse(initial.isEnabled)
        assertEquals(1, initial.proxyList.size)

        // Add proxy
        val addResult = addUseCase("proxy2.org", 8080)
        assertTrue(addResult is Result.Success)
        val added = (addResult as Result.Success).data
        assertEquals("proxy2.org", added.address)
        assertEquals(8080, added.port)

        var settings = getUseCase()
        assertEquals(2, settings.proxyList.size)

        // Enable proxy
        val enableResult = enableUseCase(added)
        assertTrue(enableResult is Result.Success)
        settings = getUseCase()
        assertTrue(settings.isEnabled)
        assertEquals(added, settings.currentProxy)

        // Ping check
        val pingResult = checkPingUseCase(added)
        assertTrue(pingResult is Result.Success)
        assertEquals(42L, (pingResult as Result.Success).data)

        // Toggle rotation
        val toggleResult = toggleRotationUseCase(true, 15)
        assertTrue(toggleResult is Result.Success)
        settings = getUseCase()
        assertTrue(settings.isRotationEnabled)
        assertEquals(15, settings.rotationTimeoutMinutes)

        // Disable proxy
        val disableResult = disableUseCase()
        assertTrue(disableResult is Result.Success)
        settings = getUseCase()
        assertFalse(settings.isEnabled)

        // Delete proxy
        val deleteResult = deleteUseCase(added)
        assertTrue(deleteResult is Result.Success)
        settings = getUseCase()
        assertEquals(1, settings.proxyList.size)
    }

    @Test
    fun testProxyViewModelWorkflow() = runTest {
        val fakeRepo = FakeProxyRepository()

        val viewModel = ProxyViewModel(
            observeProxySettingsUseCase = ObserveProxySettingsUseCase(fakeRepo),
            getProxySettingsUseCase = GetProxySettingsUseCase(fakeRepo),
            addProxyUseCase = AddProxyUseCase(fakeRepo),
            deleteProxyUseCase = DeleteProxyUseCase(fakeRepo),
            enableProxyUseCase = EnableProxyUseCase(fakeRepo),
            disableProxyUseCase = DisableProxyUseCase(fakeRepo),
            toggleProxyRotationUseCase = ToggleProxyRotationUseCase(fakeRepo),
            checkProxyPingUseCase = CheckProxyPingUseCase(fakeRepo)
        )

        advanceUntilIdle()

        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is ProxyUiState.Success)
        val successState = loadedState as ProxyUiState.Success
        assertFalse(successState.settings.isEnabled)
        assertEquals(1, successState.settings.proxyList.size)

        // Add Proxy via ViewModel
        viewModel.onEvent(ProxyEvent.AddProxy("1.2.3.4", 9000))
        advanceUntilIdle()

        val afterAddState = viewModel.uiState.value as ProxyUiState.Success
        assertEquals(2, afterAddState.settings.proxyList.size)

        val newProxy = afterAddState.settings.proxyList.first { it.address == "1.2.3.4" }

        // Enable Proxy
        viewModel.onEvent(ProxyEvent.EnableProxy(newProxy))
        advanceUntilIdle()

        val afterEnableState = viewModel.uiState.value as ProxyUiState.Success
        assertTrue(afterEnableState.settings.isEnabled)
        assertEquals(newProxy, afterEnableState.settings.currentProxy)

        // Check Ping
        viewModel.onEvent(ProxyEvent.CheckPing(newProxy))
        advanceUntilIdle()

        val afterPingState = viewModel.uiState.value as ProxyUiState.Success
        assertNull(afterPingState.checkingProxy)

        // Toggle Rotation
        viewModel.onEvent(ProxyEvent.ToggleRotation(true, 30))
        advanceUntilIdle()

        val afterRotationState = viewModel.uiState.value as ProxyUiState.Success
        assertTrue(afterRotationState.settings.isRotationEnabled)
        assertEquals(30, afterRotationState.settings.rotationTimeoutMinutes)

        // Disable Proxy
        viewModel.onEvent(ProxyEvent.DisableProxy)
        advanceUntilIdle()

        val afterDisableState = viewModel.uiState.value as ProxyUiState.Success
        assertFalse(afterDisableState.settings.isEnabled)

        // Delete Proxy
        viewModel.onEvent(ProxyEvent.DeleteProxy(newProxy))
        advanceUntilIdle()

        val afterDeleteState = viewModel.uiState.value as ProxyUiState.Success
        assertEquals(1, afterDeleteState.settings.proxyList.size)

        // Clear Error
        viewModel.onEvent(ProxyEvent.ClearError)
        val finalState = viewModel.uiState.value as ProxyUiState.Success
        assertNull(finalState.error)
    }

    private class FakeProxyRepository : ProxyRepository {
        private val list = mutableListOf(
            ProxyModel("proxy1.org", 1080, "u1", "p1")
        )
        private var isEnabled = false
        private var currentProxy: ProxyModel? = null
        private var isRotationEnabled = false
        private var rotationTimeoutMinutes = 10
        private var useCallsWithProxy = false

        private val stateFlow = MutableStateFlow(buildSettings())

        private fun buildSettings(): ProxySettingsModel {
            return ProxySettingsModel(
                isEnabled = isEnabled,
                currentProxy = currentProxy,
                proxyList = list.toList(),
                isRotationEnabled = isRotationEnabled,
                rotationTimeoutMinutes = rotationTimeoutMinutes,
                useCallsWithProxy = useCallsWithProxy
            )
        }

        private fun notifyChange() {
            stateFlow.value = buildSettings()
        }

        override fun observeProxySettings(): Flow<ProxySettingsModel> = stateFlow.asStateFlow()

        override suspend fun getProxySettings(): ProxySettingsModel = buildSettings()

        override suspend fun addProxy(
            address: String,
            port: Int,
            username: String,
            password: String,
            secret: String
        ): Result<ProxyModel> {
            val model = ProxyModel(
                address = address,
                port = port,
                username = username,
                password = password,
                secret = secret
            )
            list.add(0, model)
            notifyChange()
            return Result.Success(model)
        }

        override suspend fun deleteProxy(proxy: ProxyModel): Result<Unit> {
            list.removeAll { it.address == proxy.address && it.port == proxy.port }
            if (currentProxy?.address == proxy.address && currentProxy?.port == proxy.port) {
                currentProxy = null
                isEnabled = false
            }
            notifyChange()
            return Result.Success(Unit)
        }

        override suspend fun enableProxy(proxy: ProxyModel): Result<Unit> {
            currentProxy = proxy
            isEnabled = true
            notifyChange()
            return Result.Success(Unit)
        }

        override suspend fun disableProxy(): Result<Unit> {
            isEnabled = false
            notifyChange()
            return Result.Success(Unit)
        }

        override suspend fun toggleProxyRotation(
            enabled: Boolean,
            timeoutMinutes: Int
        ): Result<Unit> {
            isRotationEnabled = enabled
            rotationTimeoutMinutes = timeoutMinutes
            notifyChange()
            return Result.Success(Unit)
        }

        override suspend fun checkProxyPing(proxy: ProxyModel): Result<Long> {
            val index = list.indexOfFirst { it.address == proxy.address && it.port == proxy.port }
            if (index >= 0) {
                list[index] = list[index].copy(ping = 42L, isAvailable = true, isChecking = false)
                notifyChange()
            }
            return Result.Success(42L)
        }
    }
}
