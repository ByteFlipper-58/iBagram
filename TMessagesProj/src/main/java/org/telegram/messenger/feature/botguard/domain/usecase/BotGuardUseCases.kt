package org.telegram.messenger.feature.botguard.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.botguard.domain.model.BotGuardBulletinInfo
import org.telegram.messenger.feature.botguard.domain.model.BotGuardBulletinType
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionStatus
import org.telegram.messenger.feature.botguard.domain.model.BotGuardLaunchDecision
import org.telegram.messenger.feature.botguard.domain.model.BotGuardSession
import org.telegram.messenger.feature.botguard.domain.model.BotGuardState
import org.telegram.messenger.feature.botguard.domain.repository.BotGuardRepository

class IsGuardBotConfirmationNeededUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(guardBotId: Long): Boolean {
        if (repository.isBotWhitelisted(guardBotId)) {
            return false
        }
        if (repository.isConfirmationShown(guardBotId)) {
            return false
        }
        return true
    }
}

class DetermineGuardBotLaunchFlowUseCase(
    private val isConfirmationNeeded: IsGuardBotConfirmationNeededUseCase
) {
    operator fun invoke(
        dialogId: Long,
        guardBotId: Long,
        queryId: Long,
        alreadyConfirmed: Boolean = false
    ): BotGuardLaunchDecision {
        if (alreadyConfirmed) {
            return BotGuardLaunchDecision.LaunchDirectly
        }
        return if (isConfirmationNeeded(guardBotId)) {
            BotGuardLaunchDecision.PromptConfirmation(dialogId, guardBotId, queryId)
        } else {
            BotGuardLaunchDecision.LaunchDirectly
        }
    }
}

class RegisterGuardBotSessionUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(
        dialogId: Long,
        guardBotId: Long,
        queryId: Long,
        isConfirmed: Boolean = true
    ): BotGuardSession {
        return repository.registerSession(dialogId, guardBotId, queryId, isConfirmed)
    }
}

class GetGuardBotSessionUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(queryId: Long): BotGuardSession? {
        return repository.getSession(queryId)
    }
}

class GetAllActiveGuardBotSessionsUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(): List<BotGuardSession> {
        return repository.getAllActiveSessions()
    }
}

class CloseGuardBotSessionUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(
        dialogId: Long,
        queryId: Long,
        status: BotGuardDecisionStatus
    ): BotGuardDecisionResult {
        val existingSession = repository.removeSession(queryId)
        val guardBotId = existingSession?.guardBotId ?: 0L
        val decision = BotGuardDecisionResult(
            dialogId = if (dialogId != 0L) dialogId else existingSession?.dialogId ?: 0L,
            guardBotId = guardBotId,
            queryId = queryId,
            status = status
        )
        repository.postDecision(decision)
        return decision
    }
}

class SetGuardBotConfirmationShownUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(guardBotId: Long, shown: Boolean = true) {
        repository.setConfirmationShown(guardBotId, shown)
    }
}

class ClearAllGuardBotSessionsUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke() {
        repository.clearAllSessions()
    }
}

class ObserveGuardBotDecisionsUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(): Flow<BotGuardDecisionResult> {
        return repository.observeDecisions()
    }
}

class ObserveGuardBotStateUseCase(
    private val repository: BotGuardRepository
) {
    operator fun invoke(): StateFlow<BotGuardState> {
        return repository.observeState()
    }
}

class MapJoinChatBotResultUseCase {
    operator fun invoke(constructorId: Int, webViewUrl: String? = null): BotGuardDecisionStatus {
        return when (constructorId) {
            0xAE152A69.toInt() -> BotGuardDecisionStatus.Approved
            0x0EFA0194.toInt() -> BotGuardDecisionStatus.Declined
            0x98A3A840.toInt() -> BotGuardDecisionStatus.Queued
            0xD6E3B813.toInt() -> BotGuardDecisionStatus.WebView(webViewUrl ?: "")
            else -> BotGuardDecisionStatus.Unknown(constructorId)
        }
    }
}

class FormatGuardBotBulletinUseCase {
    operator fun invoke(result: BotGuardDecisionResult, chatName: String): BotGuardBulletinInfo {
        val type = when (result.status) {
            is BotGuardDecisionStatus.Approved -> BotGuardBulletinType.APPROVED
            is BotGuardDecisionStatus.Declined -> BotGuardBulletinType.DECLINED
            is BotGuardDecisionStatus.Queued -> BotGuardBulletinType.QUEUED
            else -> BotGuardBulletinType.NONE
        }

        val messageKey = when (type) {
            BotGuardBulletinType.APPROVED -> "GuardBotJoinRequestApproved"
            BotGuardBulletinType.DECLINED -> "GuardBotJoinRequestDeclined"
            BotGuardBulletinType.QUEUED -> "GuardBotJoinRequestQueued"
            BotGuardBulletinType.NONE -> ""
        }

        return BotGuardBulletinInfo(
            type = type,
            messageKey = messageKey,
            chatName = chatName
        )
    }
}
