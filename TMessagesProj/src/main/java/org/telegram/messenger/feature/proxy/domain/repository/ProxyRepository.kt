package org.telegram.messenger.feature.proxy.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.proxy.domain.model.ProxySettingsModel

interface ProxyRepository {
    fun observeProxySettings(): Flow<ProxySettingsModel>
    suspend fun getProxySettings(): ProxySettingsModel
    suspend fun addProxy(
        address: String,
        port: Int,
        username: String = "",
        password: String = "",
        secret: String = ""
    ): Result<ProxyModel>
    suspend fun deleteProxy(proxy: ProxyModel): Result<Unit>
    suspend fun enableProxy(proxy: ProxyModel): Result<Unit>
    suspend fun disableProxy(): Result<Unit>
    suspend fun toggleProxyRotation(enabled: Boolean, timeoutMinutes: Int = 10): Result<Unit>
    suspend fun checkProxyPing(proxy: ProxyModel): Result<Long>
}
