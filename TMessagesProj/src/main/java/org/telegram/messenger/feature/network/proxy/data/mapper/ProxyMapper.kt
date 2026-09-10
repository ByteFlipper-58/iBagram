package org.telegram.messenger.feature.network.proxy.data.mapper

import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel

object ProxyMapper {

    fun toDomain(info: SharedConfig.ProxyInfo): ProxyModel {
        return ProxyModel(
            address = info.address ?: "",
            port = info.port,
            username = info.username ?: "",
            password = info.password ?: "",
            secret = info.secret ?: "",
            ping = info.ping,
            isAvailable = info.available,
            isChecking = info.checking,
        )
    }

    fun toLegacy(model: ProxyModel): SharedConfig.ProxyInfo {
        val info = SharedConfig.ProxyInfo(
            model.address,
            model.port,
            model.username,
            model.password,
            model.secret
        )
        info.ping = model.ping
        info.available = model.isAvailable
        info.checking = model.isChecking
        return info
    }

    fun toSettingsDomain(
        isEnabled: Boolean,
        currentProxy: SharedConfig.ProxyInfo?,
        proxyList: List<SharedConfig.ProxyInfo>,
        isRotationEnabled: Boolean,
        rotationTimeout: Int,
        useCallsWithProxy: Boolean,
    ): ProxySettingsModel {
        return ProxySettingsModel(
            isEnabled = isEnabled,
            currentProxy = currentProxy?.let { toDomain(it) },
            proxyList = proxyList.map { toDomain(it) },
            isRotationEnabled = isRotationEnabled,
            rotationTimeoutMinutes = rotationTimeout,
            useCallsWithProxy = useCallsWithProxy,
        )
    }
}
