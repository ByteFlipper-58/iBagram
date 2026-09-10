package org.telegram.messenger.feature.botguard.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.BotGuardHelper
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SharedPrefsHelper
import org.telegram.messenger.feature.botguard.data.mapper.BotGuardMapper
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.botguard.domain.model.BotGuardSession
import org.telegram.messenger.feature.botguard.domain.model.BotGuardState
import org.telegram.messenger.feature.botguard.domain.repository.BotGuardRepository
import java.util.concurrent.ConcurrentHashMap

class LegacyBotGuardRepository(
    private val currentAccount: Int
) : BotGuardRepository, NotificationCenter.NotificationCenterDelegate {

    private val activeSessions = ConcurrentHashMap<Long, BotGuardSession>()
    private val inMemoryConfirmedBots = ConcurrentHashMap.newKeySet<Long>()
    private val inMemoryWhitelistedBots = ConcurrentHashMap.newKeySet<Long>()

    private val _state = MutableStateFlow(BotGuardState())
    private val _decisions = MutableSharedFlow<BotGuardDecisionResult>(extraBufferCapacity = 64)

    init {
        runCatching {
            NotificationCenter.getInstance(currentAccount)?.addObserver(
                this,
                NotificationCenter.guardBotDecisionResult
            )
        }
    }

    override fun getSession(queryId: Long): BotGuardSession? {
        return activeSessions[queryId]
    }

    override fun getAllActiveSessions(): List<BotGuardSession> {
        return activeSessions.values.toList()
    }

    override fun registerSession(
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
        _state.update { current ->
            current.copy(activeSessions = HashMap(activeSessions))
        }
        return session
    }

    override fun removeSession(queryId: Long): BotGuardSession? {
        val removed = activeSessions.remove(queryId)
        _state.update { current ->
            current.copy(activeSessions = HashMap(activeSessions))
        }
        return removed
    }

    override fun clearAllSessions() {
        activeSessions.clear()
        _state.update { current ->
            current.copy(activeSessions = emptyMap())
        }
    }

    override fun isConfirmationShown(guardBotId: Long): Boolean {
        val prefsResult = runCatching {
            SharedPrefsHelper.isWebViewConfirmShown(currentAccount, guardBotId)
        }.getOrNull()

        return prefsResult ?: inMemoryConfirmedBots.contains(guardBotId)
    }

    override fun setConfirmationShown(guardBotId: Long, shown: Boolean) {
        if (shown) {
            inMemoryConfirmedBots.add(guardBotId)
        } else {
            inMemoryConfirmedBots.remove(guardBotId)
        }
        runCatching {
            SharedPrefsHelper.setWebViewConfirmShown(currentAccount, guardBotId, shown)
        }
    }

    override fun isBotWhitelisted(guardBotId: Long): Boolean {
        val controllerResult = runCatching {
            MessagesController.getInstance(currentAccount)?.whitelistedBots?.contains(guardBotId)
        }.getOrNull()

        return controllerResult ?: inMemoryWhitelistedBots.contains(guardBotId)
    }

    fun setBotWhitelistedInMemory(guardBotId: Long, whitelisted: Boolean) {
        if (whitelisted) {
            inMemoryWhitelistedBots.add(guardBotId)
        } else {
            inMemoryWhitelistedBots.remove(guardBotId)
        }
    }

    override fun observeDecisions(): Flow<BotGuardDecisionResult> {
        return _decisions.asSharedFlow()
    }

    override fun postDecision(decision: BotGuardDecisionResult) {
        _decisions.tryEmit(decision)
        _state.update { current ->
            current.copy(lastDecision = decision)
        }
        runCatching {
            val notification = BotGuardMapper.toNotification(decision)
            NotificationCenter.getInstance(currentAccount)?.postNotificationName(
                NotificationCenter.guardBotDecisionResult,
                notification
            )
        }
    }

    override fun observeState(): StateFlow<BotGuardState> {
        return _state.asStateFlow()
    }

    override fun getCurrentState(): BotGuardState {
        return _state.value
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.guardBotDecisionResult && args.isNotEmpty()) {
            val notification = args[0] as? BotGuardHelper.GuardBotDecisionResultNotification ?: return
            val decision = BotGuardMapper.fromNotification(notification)
            activeSessions.remove(decision.queryId)
            _decisions.tryEmit(decision)
            _state.update { current ->
                current.copy(
                    activeSessions = HashMap(activeSessions),
                    lastDecision = decision
                )
            }
        }
    }
}
