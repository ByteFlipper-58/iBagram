package org.telegram.messenger.feature.business.botstars.presentation

import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.business.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.business.botstars.domain.model.StarRefProgramModel

data class BotStarsUiState(
    val dialogId: Long = 0L,
    val starsStats: BotStarsRevenueStatsModel? = null,
    val tonStats: BotStarsRevenueStatsModel? = null,
    val selectedTransactionType: BotStarsTransactionType = BotStarsTransactionType.ALL,
    val transactions: List<BotStarsTransactionModel> = emptyList(),
    val connectedBots: List<ConnectedBotStarRefModel> = emptyList(),
    val suggestedBots: List<StarRefProgramModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentBalance: Long
        get() = starsStats?.currentBalance ?: 0L

    val tonBalance: Long
        get() = tonStats?.currentBalance ?: 0L

    val availableBalance: Long
        get() = starsStats?.availableBalance ?: 0L

    val overallRevenue: Long
        get() = starsStats?.overallRevenue ?: 0L

    val hasTransactions: Boolean
        get() = transactions.isNotEmpty()

    val connectedBotsCount: Int
        get() = connectedBots.count { it.isActive }
}
