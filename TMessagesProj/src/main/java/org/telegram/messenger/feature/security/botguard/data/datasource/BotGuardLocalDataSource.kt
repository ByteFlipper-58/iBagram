package org.telegram.messenger.feature.security.botguard.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.SharedPrefsHelper
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Local data source managing Guard Bot active sessions, confirmation dialog states, and bot whitelists.
 */
open class BotGuardLocalDataSource(
    private val currentAccount: Int
) {
    private val activeSessions = ConcurrentHashMap<Long, BotGuardSession>()
    private val inMemoryConfirmedBots = ConcurrentHashMap.newKeySet<Long>()
    private val inMemoryWhitelistedBots = ConcurrentHashMap.newKeySet<Long>()

    open fun getSession(queryId: Long): BotGuardSession? {
        return activeSessions[queryId]
    }

    open fun getAllActiveSessions(): List<BotGuardSession> {
        return activeSessions.values.toList()
    }

    open fun registerSession(
        dialogId: Long,
        guardBotId: Long,
        queryId: Long,
        isConfirmed: Boolean
    ): BotGuardSession {
        val session = BotGuardSession(
            dialogId = dialogId,
            guardBotId = guardBotId,
            queryId = queryId,
            isConfirmed = isConfirmed
        )
        activeSessions[queryId] = session
        return session
    }

    open fun removeSession(queryId: Long): BotGuardSession? {
        return activeSessions.remove(queryId)
    }

    open fun clearAllSessions() {
        activeSessions.clear()
    }

    open fun isConfirmationShown(guardBotId: Long): Boolean {
        val prefsResult = runCatching {
            SharedPrefsHelper.isWebViewConfirmShown(currentAccount, guardBotId)
        }.getOrNull()

        return prefsResult ?: inMemoryConfirmedBots.contains(guardBotId)
    }

    open fun setConfirmationShown(guardBotId: Long, shown: Boolean) {
        if (shown) {
            inMemoryConfirmedBots.add(guardBotId)
        } else {
            inMemoryConfirmedBots.remove(guardBotId)
        }
        runCatching {
            SharedPrefsHelper.setWebViewConfirmShown(currentAccount, guardBotId, shown)
        }
    }

    open fun isBotWhitelisted(guardBotId: Long): Boolean {
        val controllerResult = runCatching {
            MessagesController.getInstance(currentAccount)?.whitelistedBots?.contains(guardBotId)
        }.getOrNull()

        return controllerResult ?: inMemoryWhitelistedBots.contains(guardBotId)
    }

    open fun setBotWhitelistedInMemory(guardBotId: Long, whitelisted: Boolean) {
        if (whitelisted) {
            inMemoryWhitelistedBots.add(guardBotId)
        } else {
            inMemoryWhitelistedBots.remove(guardBotId)
        }
    }

    open fun getActiveSessionsMap(): Map<Long, BotGuardSession> {
        return HashMap(activeSessions)
    }
}
