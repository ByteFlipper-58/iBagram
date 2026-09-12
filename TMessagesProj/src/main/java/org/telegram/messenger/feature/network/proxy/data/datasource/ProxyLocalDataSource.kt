package org.telegram.messenger.feature.network.proxy.data.datasource

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedConfig

/**
 * Local data source managing proxy list persistence, current active proxy,
 * rotation preferences, and SharedPreferences storage.
 */
open class ProxyLocalDataSource(
    protected val currentAccount: Int,
    protected val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    protected val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Headless in-memory fallback state for unit tests
    private val memoryProxyList = mutableListOf<SharedConfig.ProxyInfo>()
    private var memoryCurrentProxy: SharedConfig.ProxyInfo? = null
    private var memoryProxyEnabled = false
    private var memoryRotationEnabled = false
    private var memoryRotationTimeoutIndex = 1
    private var memoryCallsWithProxy = false

    private fun getPreferences(): SharedPreferences? {
        return try {
            MessagesController.getGlobalMainSettings()
        } catch (_: Throwable) {
            null
        }
    }

    open fun loadProxyList() {
        try {
            SharedConfig.loadProxyList()
        } catch (_: Throwable) {
        }
    }

    open fun getProxyList(): List<SharedConfig.ProxyInfo> {
        return try {
            SharedConfig.proxyList ?: memoryProxyList
        } catch (_: Throwable) {
            memoryProxyList
        }
    }

    open fun getCurrentProxy(): SharedConfig.ProxyInfo? {
        return try {
            SharedConfig.currentProxy ?: memoryCurrentProxy
        } catch (_: Throwable) {
            memoryCurrentProxy
        }
    }

    open fun isProxyEnabled(): Boolean {
        return try {
            SharedConfig.isProxyEnabled()
        } catch (_: Throwable) {
            memoryProxyEnabled
        }
    }

    open fun isProxyRotationEnabled(): Boolean {
        return try {
            SharedConfig.proxyRotationEnabled
        } catch (_: Throwable) {
            memoryRotationEnabled
        }
    }

    open fun getProxyRotationTimeoutIndex(): Int {
        return try {
            SharedConfig.proxyRotationTimeout
        } catch (_: Throwable) {
            memoryRotationTimeoutIndex
        }
    }

    open fun getUseCallsWithProxy(): Boolean {
        return try {
            getPreferences()?.getBoolean("proxy_enabled_calls", false) ?: memoryCallsWithProxy
        } catch (_: Throwable) {
            memoryCallsWithProxy
        }
    }

    open suspend fun addProxy(info: SharedConfig.ProxyInfo): SharedConfig.ProxyInfo = withContext(mainDispatcher) {
        try {
            loadProxyList()
            SharedConfig.addProxy(info)
        } catch (_: Throwable) {
            memoryProxyList.removeAll { it.address == info.address && it.port == info.port && it.secret == info.secret }
            memoryProxyList.add(info)
            info
        }
    }

    open suspend fun deleteProxy(info: SharedConfig.ProxyInfo) = withContext(mainDispatcher) {
        try {
            loadProxyList()
            SharedConfig.deleteProxy(info)
        } catch (_: Throwable) {
            memoryProxyList.removeAll { it.address == info.address && it.port == info.port && it.secret == info.secret }
            if (memoryCurrentProxy?.address == info.address && memoryCurrentProxy?.port == info.port) {
                memoryCurrentProxy = null
            }
        }
    }

    open suspend fun saveProxyPreferences(
        enabled: Boolean,
        proxyInfo: SharedConfig.ProxyInfo?
    ) = withContext(mainDispatcher) {
        try {
            SharedConfig.currentProxy = proxyInfo
            val preferences = getPreferences()
            if (preferences != null) {
                val editor = preferences.edit()
                if (proxyInfo != null) {
                    editor.putString("proxy_ip", proxyInfo.address)
                    editor.putInt("proxy_port", proxyInfo.port)
                    editor.putString("proxy_user", proxyInfo.username)
                    editor.putString("proxy_pass", proxyInfo.password)
                    editor.putString("proxy_secret", proxyInfo.secret)
                    editor.putBoolean("proxy_enabled", enabled)
                    if (!proxyInfo.secret.isNullOrEmpty()) {
                        editor.putBoolean("proxy_enabled_calls", false)
                    }
                } else {
                    editor.putBoolean("proxy_enabled", false)
                }
                editor.apply()
            }
        } catch (_: Throwable) {
            memoryCurrentProxy = proxyInfo
            memoryProxyEnabled = enabled
        }
    }

    open suspend fun saveRotationSettings(
        enabled: Boolean,
        timeoutIndex: Int
    ) = withContext(mainDispatcher) {
        try {
            SharedConfig.proxyRotationEnabled = enabled
            SharedConfig.proxyRotationTimeout = timeoutIndex
            val preferences = getPreferences()
            if (preferences != null) {
                preferences.edit()
                    .putBoolean("proxyRotationEnabled", enabled)
                    .putInt("proxyRotationTimeout", timeoutIndex)
                    .apply()
            }
        } catch (_: Throwable) {
            memoryRotationEnabled = enabled
            memoryRotationTimeoutIndex = timeoutIndex
        }
    }

    open fun notifyProxySettingsChanged() {
        try {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged)
        } catch (_: Throwable) {
        }
    }

    open fun notifyProxyCheckDone(info: SharedConfig.ProxyInfo) {
        try {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, info)
        } catch (_: Throwable) {
        }
    }

    open fun notifyProxyChangedByRotation() {
        try {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyChangedByRotation)
        } catch (_: Throwable) {
        }
    }
}
