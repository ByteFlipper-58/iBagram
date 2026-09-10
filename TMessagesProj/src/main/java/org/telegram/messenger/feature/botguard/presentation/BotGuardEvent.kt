package org.telegram.messenger.feature.botguard.presentation

import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionStatus

sealed class BotGuardEvent {
    data class RequestOpenGuardBot(
        val dialogId: Long,
        val guardBotId: Long,
        val queryId: Long,
        val chatName: String = ""
    ) : BotGuardEvent()

    data class ConfirmLaunch(
        val dialogId: Long,
        val guardBotId: Long,
        val queryId: Long
    ) : BotGuardEvent()

    object DismissConfirmation : BotGuardEvent()

    data class DecisionReceived(
        val dialogId: Long,
        val guardBotId: Long,
        val queryId: Long,
        val status: BotGuardDecisionStatus,
        val chatName: String = ""
    ) : BotGuardEvent()

    data class CloseSession(val queryId: Long) : BotGuardEvent()

    object ClearPendingActions : BotGuardEvent()
}
