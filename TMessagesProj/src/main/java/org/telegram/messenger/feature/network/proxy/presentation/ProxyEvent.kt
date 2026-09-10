package org.telegram.messenger.feature.network.proxy.presentation

import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel

sealed interface ProxyEvent {
    data object LoadSettings : ProxyEvent
    data class AddProxy(
        val address: String,
        val port: Int,
        val username: String = "",
        val password: String = "",
        val secret: String = ""
    ) : ProxyEvent
    data class DeleteProxy(val proxy: ProxyModel) : ProxyEvent
    data class EnableProxy(val proxy: ProxyModel) : ProxyEvent
    data object DisableProxy : ProxyEvent
    data class ToggleRotation(val enabled: Boolean, val timeoutMinutes: Int = 10) : ProxyEvent
    data class CheckPing(val proxy: ProxyModel) : ProxyEvent
    data object ClearError : ProxyEvent
}
