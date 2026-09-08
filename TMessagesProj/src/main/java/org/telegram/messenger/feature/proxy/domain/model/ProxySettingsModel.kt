package org.telegram.messenger.feature.proxy.domain.model

data class ProxySettingsModel(
    val isEnabled: Boolean = false,
    val currentProxy: ProxyModel? = null,
    val proxyList: List<ProxyModel> = emptyList(),
    val isRotationEnabled: Boolean = false,
    val rotationTimeoutMinutes: Int = 10,
    val useCallsWithProxy: Boolean = false,
)
