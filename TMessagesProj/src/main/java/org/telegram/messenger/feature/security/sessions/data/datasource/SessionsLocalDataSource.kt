package org.telegram.messenger.feature.security.sessions.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.tgnet.ConnectionsManager
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel
import java.util.Base64

/**
 * Local data source managing memory caches for device and web sessions,
 * QR login token decoding, and local push token refreshing.
 */
open class SessionsLocalDataSource(
    private val currentAccount: Int
) {
    private val lock = Any()

    @Volatile
    private var cachedSessions: SessionsListModel = SessionsListModel()

    @Volatile
    private var cachedWebSessions: List<WebSessionModel> = emptyList()

    open fun getCachedSessions(): SessionsListModel = synchronized(lock) {
        cachedSessions
    }

    open fun putCachedSessions(sessions: SessionsListModel) = synchronized(lock) {
        cachedSessions = sessions
    }

    open fun getCachedWebSessions(): List<WebSessionModel> = synchronized(lock) {
        cachedWebSessions
    }

    open fun putCachedWebSessions(sessions: List<WebSessionModel>) = synchronized(lock) {
        cachedWebSessions = sessions
    }

    open fun removeSession(hash: Long) = synchronized(lock) {
        cachedSessions = cachedSessions.copy(
            otherSessions = cachedSessions.otherSessions.filter { it.hash != hash },
            passwordPendingSessions = cachedSessions.passwordPendingSessions.filter { it.hash != hash }
        )
    }

    open fun clearOtherSessions() = synchronized(lock) {
        cachedSessions = cachedSessions.copy(
            otherSessions = emptyList(),
            passwordPendingSessions = emptyList()
        )
    }

    open fun removeWebSession(hash: Long) = synchronized(lock) {
        cachedWebSessions = cachedWebSessions.filter { it.hash != hash }
    }

    open fun clearAllWebSessions() = synchronized(lock) {
        cachedWebSessions = emptyList()
    }

    open fun updateSessionSettings(hash: Long, acceptSecretChats: Boolean, acceptCalls: Boolean) = synchronized(lock) {
        cachedSessions = cachedSessions.copy(
            otherSessions = cachedSessions.otherSessions.map { session ->
                if (session.hash == hash) {
                    session.copy(acceptSecretChats = acceptSecretChats, acceptCalls = acceptCalls)
                } else {
                    session
                }
            }
        )
    }

    open fun setSessionsTtl(ttlDays: Int) = synchronized(lock) {
        cachedSessions = cachedSessions.copy(ttlDays = ttlDays)
    }

    open fun decodeTokenFromLink(link: String): ByteArray {
        var code = link.trim()
        val prefix = "tg://login?token="
        if (code.startsWith(prefix, ignoreCase = true)) {
            code = code.substring(prefix.length)
        } else if (code.contains("token=", ignoreCase = true)) {
            code = code.substringAfter("token=")
        }
        val clean = code.replace('/', '_').replace('+', '-')
        return try {
            Base64.getUrlDecoder().decode(clean)
        } catch (e: Throwable) {
            try {
                android.util.Base64.decode(clean, android.util.Base64.URL_SAFE)
            } catch (e2: Throwable) {
                Base64.getDecoder().decode(clean)
            }
        }
    }

    open fun refreshPushTokensAfterResetAll() {
        runCatching {
            for (a in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                val userConfig = UserConfig.getInstance(a) ?: continue
                if (!userConfig.isClientActivated) {
                    continue
                }
                userConfig.registeredForPush = false
                userConfig.saveConfig(false)
                MessagesController.getInstance(a)?.registerForPush(SharedConfig.pushType, SharedConfig.pushString)
                ConnectionsManager.getInstance(a)?.setUserId(userConfig.clientUserId)
            }
        }
    }
}
