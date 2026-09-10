package org.telegram.messenger.feature.botguard.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.botguard.domain.model.BotGuardSession
import org.telegram.messenger.feature.botguard.domain.model.BotGuardState

interface BotGuardRepository {
    fun getSession(queryId: Long): BotGuardSession?
    fun getAllActiveSessions(): List<BotGuardSession>
    fun registerSession(dialogId: Long, guardBotId: Long, queryId: Long, isConfirmed: Boolean): BotGuardSession
    fun removeSession(queryId: Long): BotGuardSession?
    fun clearAllSessions()

    fun isConfirmationShown(guardBotId: Long): Boolean
    fun setConfirmationShown(guardBotId: Long, shown: Boolean)
    fun isBotWhitelisted(guardBotId: Long): Boolean

    fun observeDecisions(): Flow<BotGuardDecisionResult>
    fun postDecision(decision: BotGuardDecisionResult)

    fun observeState(): StateFlow<BotGuardState>
    fun getCurrentState(): BotGuardState
}
