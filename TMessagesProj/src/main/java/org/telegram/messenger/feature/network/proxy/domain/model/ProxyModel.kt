package org.telegram.messenger.feature.network.proxy.domain.model

data class ProxyModel(
    val address: String,
    val port: Int,
    val username: String = "",
    val password: String = "",
    val secret: String = "",
    val ping: Long = 0L,
    val isAvailable: Boolean = false,
    val isChecking: Boolean = false,
) {
    val type: ProxyType
        get() = when {
            secret.isNotEmpty() -> ProxyType.MTPROTO
            else -> ProxyType.SOCKS5
        }

    val link: String
        get() = if (secret.isNotEmpty()) {
            "https://t.me/proxy?server=$address&port=$port&secret=$secret"
        } else {
            val userParam = if (username.isNotEmpty()) "&user=$username" else ""
            val passParam = if (password.isNotEmpty()) "&pass=$password" else ""
            "https://t.me/socks?server=$address&port=$port$userParam$passParam"
        }
}
