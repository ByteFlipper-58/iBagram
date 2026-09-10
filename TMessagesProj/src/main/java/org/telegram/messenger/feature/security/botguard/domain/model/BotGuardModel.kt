package org.telegram.messenger.feature.security.botguard.domain.model

sealed class BotGuardDecisionStatus {
    object Approved : BotGuardDecisionStatus()
    object Declined : BotGuardDecisionStatus()
    object Queued : BotGuardDecisionStatus()
    data class WebView(val url: String) : BotGuardDecisionStatus()
    object Dismissed : BotGuardDecisionStatus()
    data class Unknown(val constructorId: Int = 0) : BotGuardDecisionStatus()
}

data class BotGuardDecisionResult(
    val dialogId: Long,
    val guardBotId: Long,
    val queryId: Long,
    val status: BotGuardDecisionStatus
)

data class BotGuardSession(
    val dialogId: Long,
    val guardBotId: Long,
    val queryId: Long,
    val isConfirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class BotGuardLaunchDecision {
    object LaunchDirectly : BotGuardLaunchDecision()
    data class PromptConfirmation(
        val dialogId: Long,
        val guardBotId: Long,
        val queryId: Long
    ) : BotGuardLaunchDecision()
}

enum class BotGuardBulletinType {
    APPROVED,
    DECLINED,
    QUEUED,
    NONE
}

data class BotGuardBulletinInfo(
    val type: BotGuardBulletinType,
    val messageKey: String,
    val chatName: String
)

data class BotGuardState(
    val activeSessions: Map<Long, BotGuardSession> = emptyMap(),
    val lastDecision: BotGuardDecisionResult? = null
)
