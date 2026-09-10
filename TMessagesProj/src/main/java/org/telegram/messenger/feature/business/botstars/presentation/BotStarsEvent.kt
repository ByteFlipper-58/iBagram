package org.telegram.messenger.feature.business.botstars.presentation

import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType

sealed interface BotStarsEvent {
    data class SetDialogId(val dialogId: Long) : BotStarsEvent
    data class RefreshStats(val force: Boolean = true) : BotStarsEvent
    data class SelectTransactionType(val type: BotStarsTransactionType) : BotStarsEvent
    data class LoadTransactions(val reload: Boolean = false) : BotStarsEvent
    data class LoadConnectedBots(val reload: Boolean = false) : BotStarsEvent
    data class LoadSuggestedBots(val sort: Int = 0) : BotStarsEvent
    object ClearError : BotStarsEvent
}
