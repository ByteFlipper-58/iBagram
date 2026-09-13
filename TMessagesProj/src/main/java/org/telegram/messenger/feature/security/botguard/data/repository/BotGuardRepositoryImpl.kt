package org.telegram.messenger.feature.security.botguard.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.BotGuardHelper
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.feature.security.botguard.data.datasource.BotGuardLocalDataSource
import org.telegram.messenger.feature.security.botguard.data.datasource.BotGuardRemoteDataSource
import org.telegram.messenger.feature.security.botguard.data.mapper.BotGuardMapper
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardSession
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardState
import org.telegram.messenger.feature.security.botguard.domain.repository.BotGuardRepository

/**
 * Clean repository implementation coordinating [BotGuardLocalDataSource] and [BotGuardRemoteDataSource].
 */
open class BotGuardRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: BotGuardLocalDataSource,
    private val remoteDataSource: BotGuardRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : BotGuardRepository, NotificationCenter.NotificationCenterDelegate {

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
        return localDataSource.getSession(queryId)
    }

    override fun getAllActiveSessions(): List<BotGuardSession> {
        return localDataSource.getAllActiveSessions()
    }

    override fun registerSession(
        dialogId: Long,
        guardBotId: Long,
        queryId: Long,
        isConfirmed: Boolean
    ): BotGuardSession {
        val session = localDataSource.registerSession(dialogId, guardBotId, queryId, isConfirmed)
        _state.update { current ->
            current.copy(activeSessions = localDataSource.getActiveSessionsMap())
        }
        return session
    }

    override fun removeSession(queryId: Long): BotGuardSession? {
        val removed = localDataSource.removeSession(queryId)
        _state.update { current ->
            current.copy(activeSessions = localDataSource.getActiveSessionsMap())
        }
        return removed
    }

    override fun clearAllSessions() {
        localDataSource.clearAllSessions()
        _state.update { current ->
            current.copy(activeSessions = emptyMap())
        }
    }

    override fun isConfirmationShown(guardBotId: Long): Boolean {
        return localDataSource.isConfirmationShown(guardBotId)
    }

    override fun setConfirmationShown(guardBotId: Long, shown: Boolean) {
        localDataSource.setConfirmationShown(guardBotId, shown)
    }

    override fun isBotWhitelisted(guardBotId: Long): Boolean {
        return localDataSource.isBotWhitelisted(guardBotId)
    }

    override fun observeDecisions(): Flow<BotGuardDecisionResult> {
        return _decisions.asSharedFlow()
    }

    override fun postDecision(decision: BotGuardDecisionResult) {
        _state.update { current ->
            current.copy(lastDecision = decision)
        }
        _decisions.tryEmit(decision)

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
        if (id == NotificationCenter.guardBotDecisionResult) {
            val notification = args.firstOrNull() as? BotGuardHelper.GuardBotDecisionResultNotification ?: return
            val decision = BotGuardMapper.fromNotification(notification)
            _state.update { current ->
                current.copy(lastDecision = decision)
            }
            _decisions.tryEmit(decision)
        }
    }
}
