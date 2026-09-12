package org.telegram.messenger.feature.network.proxy.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.ProxyRotationController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyLocalDataSource
import org.telegram.messenger.feature.network.proxy.data.datasource.ProxyRemoteDataSource
import org.telegram.messenger.feature.network.proxy.data.mapper.ProxyMapper
import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

/**
 * Modern repository implementation for managing proxies and proxy rotation,
 * coordinating [ProxyLocalDataSource] and [ProxyRemoteDataSource].
 */
class ProxyRepositoryImpl(
    private val account: Int,
    private val localDataSource: ProxyLocalDataSource,
    private val remoteDataSource: ProxyRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProxyRepository {

    override fun observeProxySettings(): Flow<ProxySettingsModel> {
        val proxySettingsChangedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxySettingsChanged)
        val proxyCheckDoneFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxyCheckDone)
        val proxyRotatedFlow = NotificationCenterFlowBridge.observeGlobalEvent(NotificationCenter.proxyChangedByRotation)

        return merge(proxySettingsChangedFlow, proxyCheckDoneFlow, proxyRotatedFlow)
            .map { getProxySettings() }
            .onStart { emit(getProxySettings()) }
            .flowOn(ioDispatcher)
    }

    override suspend fun getProxySettings(): ProxySettingsModel = withContext(mainDispatcher) {
        localDataSource.loadProxyList()

        val isEnabled = localDataSource.isProxyEnabled()
        val currentProxy = localDataSource.getCurrentProxy()
        val proxyList = localDataSource.getProxyList()
        val isRotationEnabled = localDataSource.isProxyRotationEnabled()
        val rotationTimeoutIndex = localDataSource.getProxyRotationTimeoutIndex()
        val rotationTimeout = if (rotationTimeoutIndex >= 0 && rotationTimeoutIndex < ProxyRotationController.ROTATION_TIMEOUTS.size) {
            ProxyRotationController.ROTATION_TIMEOUTS[rotationTimeoutIndex]
        } else {
            10
        }
        val useCallsWithProxy = localDataSource.getUseCallsWithProxy()

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
                return@withContext Result.Failure(AppError.InvalidInput("Invalid proxy address or port"))
            }
            val info = SharedConfig.ProxyInfo(trimmedAddress, port, username, password, secret)
            val added = localDataSource.addProxy(info)
            localDataSource.notifyProxySettingsChanged()
            Result.Success(ProxyMapper.toDomain(added))
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to add proxy", e))
        }
    }

    override suspend fun deleteProxy(proxy: ProxyModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            val info = localDataSource.getProxyList().firstOrNull {
                it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
            }
            if (info != null) {
                localDataSource.deleteProxy(info)
                localDataSource.notifyProxySettingsChanged()
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to delete proxy", e))
        }
    }

    override suspend fun enableProxy(proxy: ProxyModel): Result<Unit> = withContext(mainDispatcher) {
        try {
            var info = localDataSource.getProxyList().firstOrNull {
                it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
            }
            if (info == null) {
                info = localDataSource.addProxy(ProxyMapper.toLegacy(proxy))
            }
            localDataSource.saveProxyPreferences(enabled = true, proxyInfo = info)
            remoteDataSource.applyProxySettings(
                enabled = true,
                address = info.address,
                port = info.port,
                username = info.username,
                password = info.password,
                secret = info.secret
            )
            localDataSource.notifyProxySettingsChanged()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to enable proxy", e))
        }
    }

    override suspend fun disableProxy(): Result<Unit> = withContext(mainDispatcher) {
        try {
            localDataSource.saveProxyPreferences(enabled = false, proxyInfo = null)
            remoteDataSource.applyProxySettings(enabled = false)
            localDataSource.notifyProxySettingsChanged()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to disable proxy", e))
        }
    }

    override suspend fun toggleProxyRotation(enabled: Boolean, timeoutMinutes: Int): Result<Unit> = withContext(mainDispatcher) {
        try {
            val timeoutIndex = ProxyRotationController.ROTATION_TIMEOUTS.indexOf(timeoutMinutes).let {
                if (it >= 0) it else ProxyRotationController.DEFAULT_TIMEOUT_INDEX
            }
            localDataSource.saveRotationSettings(enabled = enabled, timeoutIndex = timeoutIndex)
            localDataSource.notifyProxySettingsChanged()
            Result.Success(Unit)
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to toggle proxy rotation", e))
        }
    }

    override suspend fun checkProxyPing(proxy: ProxyModel): Result<Long> = withContext(mainDispatcher) {
        try {
            val pingResult = remoteDataSource.checkProxyPing(
                address = proxy.address,
                port = proxy.port,
                username = proxy.username,
                password = proxy.password,
                secret = proxy.secret
            )
            val info = localDataSource.getProxyList().firstOrNull {
                it.address == proxy.address && it.port == proxy.port && it.secret == proxy.secret
            }
            if (info != null) {
                info.checking = false
                when (pingResult) {
                    is Result.Success -> {
                        info.available = true
                        info.ping = pingResult.data
                    }
                    is Result.Failure -> {
                        info.available = false
                        info.ping = 0
                    }
                }
                localDataSource.notifyProxyCheckDone(info)
            }
            pingResult
        } catch (e: Throwable) {
            Result.Failure(AppError.Generic("Failed to ping proxy", e))
        }
    }
}
