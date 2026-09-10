package org.telegram.messenger.feature.network.proxy.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.ProxyRotationController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.data.mapper.ProxyMapper
import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository
import org.telegram.tgnet.ConnectionsManager
import kotlin.coroutines.resume

class LegacyProxyRepository(
    private val currentAccount: Int,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ProxyRepository {

    override fun observeProxySettings(): Flow<ProxySettingsModel> {
        val proxySettingsChangedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxySettingsChanged)
        val proxyCheckDoneFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxyCheckDone)
        val proxyRotatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxyChangedByRotation)

        return merge(proxySettingsChangedFlow, proxyCheckDoneFlow, proxyRotatedFlow)
            .map { getProxySettings() }
            .onStart { emit(getProxySettings()) }
            .flowOn(mainDispatcher)
    }

    override suspend fun getProxySettings(): ProxySettingsModel = withContext(mainDispatcher) {
        try {
            SharedConfig.loadProxyList()
        } catch (_: Throwable) {}

        val preferences = MessagesController.getGlobalMainSettings()
        val isEnabled = SharedConfig.isProxyEnabled()
        val currentProxy = SharedConfig.currentProxy
        val proxyList = SharedConfig.proxyList ?: emptyList()
        val isRotationEnabled = SharedConfig.proxyRotationEnabled
        val rotationTimeoutIndex = SharedConfig.proxyRotationTimeout
        val rotationTimeout = if (rotationTimeoutIndex >= 0 && rotationTimeoutIndex < ProxyRotationController.ROTATION_TIMEOUTS.size) {
            ProxyRotationController.ROTATION_TIMEOUTS[rotationTimeoutIndex]
        } else {
            10
        }
        val useCallsWithProxy = preferences.getBoolean("proxy_enabled_calls", false)

        ProxyMapper.toSettingsDomain(
            isEnabled = isEnabled,
            currentProxy = currentProxy,
            proxyList = proxyList,
            isRotationEnabled = isRotationEnabled,
            rotationTimeout = rotationTimeout,
            useCallsWithProxy = useCallsWithProxy
        )
    }

    override suspend fun addProxy(
        address: String,
        port: Int,
        username: String,
        password: String,
        secret: String
    ): Result<ProxyModel> = withContext(mainDispatcher) {
        try {
            val trimmedAddress = address.trim()
            if (trimmedAddress.isEmpty() || port <= 0 || port > 65535) {
                return@withContext Result.Failure(AppError.InvalidInput("Invalid address or port"))
            }
            SharedConfig.loadProxyList()
            val info = SharedConfig.ProxyInfo(trimmedAddress, port, username, password, secret)
            val added = SharedConfig.addProxy(info)
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
            Result.Success(ProxyMapper.toDomain(added))
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to add proxy", e)
        }
    }

    override suspend fun deleteProxy(proxy: ProxyModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.loadProxyList()
            val info = SharedConfig.proxyList.firstOrNull {
                it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
            }
            if (info != null) {
                SharedConfig.deleteProxy(info)
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to delete proxy", e)
        }
    }

    override suspend fun enableProxy(proxy: ProxyModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.loadProxyList()
            var info = SharedConfig.proxyList.firstOrNull {
                it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
            }
            if (info == null) {
                info = SharedConfig.addProxy(ProxyMapper.toLegacy(proxy))
            }
            SharedConfig.currentProxy = info

            val preferences = MessagesController.getGlobalMainSettings()
            val editor = preferences.edit()
            editor.putString("proxy_ip", info.address)
            editor.putInt("proxy_port", info.port)
            editor.putString("proxy_user", info.username)
            editor.putString("proxy_pass", info.password)
            editor.putString("proxy_secret", info.secret)
            editor.putBoolean("proxy_enabled", true)
            if (!info.secret.isNullOrEmpty()) {
                editor.putBoolean("proxy_enabled_calls", false)
            }
            editor.apply()

            ConnectionsManager.setProxySettings(
                true,
                info.address,
                info.port,
                info.username,
                info.password,
                info.secret
            )
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to enable proxy", e)
        }
    }

    override suspend fun disableProxy(): Result<Unit> = withContext(mainDispatcher) {
        try {
            val preferences = MessagesController.getGlobalMainSettings()
            preferences.edit().putBoolean("proxy_enabled", false).apply()
            ConnectionsManager.setProxySettings(false, "", 0, "", "", "")
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to disable proxy", e)
        }
    }

    override suspend fun toggleProxyRotation(enabled: Boolean, timeoutMinutes: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            SharedConfig.proxyRotationEnabled = enabled
            val timeoutIndex = ProxyRotationController.ROTATION_TIMEOUTS.indexOf(timeoutMinutes).let {
                if (it >= 0) it else ProxyRotationController.DEFAULT_TIMEOUT_INDEX
            }
            SharedConfig.proxyRotationTimeout = timeoutIndex

            val preferences = MessagesController.getGlobalMainSettings()
            val editor = preferences.edit()
            editor.putBoolean("proxyRotationEnabled", enabled)
            editor.putInt("proxyRotationTimeout", timeoutIndex)
            editor.apply()

            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.failure(e.message ?: "Failed to toggle rotation", e)
        }
    }

    override suspend fun checkProxyPing(proxy: ProxyModel): Result<Long> = withContext(mainDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val cm = ConnectionsManager.getInstance(currentAccount)
            val reqId = cm.checkProxy(
                proxy.address,
                proxy.port,
                proxy.username,
                proxy.password,
                proxy.secret
            ) { time ->
                AndroidUtilities.runOnUIThread {
                    SharedConfig.loadProxyList()
                    val info = SharedConfig.proxyList.firstOrNull {
                        it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
                    }
                    if (info != null) {
                        info.checking = false
                        if (time == -1L) {
                            info.available = false
                            info.ping = 0
                        } else {
                            info.available = true
                            info.ping = time
                        }
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, info)
                    }

                    if (continuation.isActive) {
                        if (time == -1L) {
                            continuation.resume(Result.failure("Proxy unavailable"))
                        } else {
                            continuation.resume(Result.Success(time))
                        }
                    }
                }
            }

            continuation.invokeOnCancellation {
                try {
                    cm.cancelRequest(reqId.toInt(), true)
                } catch (_: Throwable) {}
            }
        }
    }
}
