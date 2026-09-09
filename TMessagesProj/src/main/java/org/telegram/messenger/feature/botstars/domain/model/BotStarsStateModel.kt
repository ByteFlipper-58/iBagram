package org.telegram.messenger.feature.botstars.domain.model

data class BotStarsStateModel(
    val dialogId: Long,
    val starsStats: BotStarsRevenueStatsModel? = null,
    val tonStats: BotStarsRevenueStatsModel? = null,
    val transactions: List<BotStarsTransactionModel> = emptyList(),
    val connectedBots: List<ConnectedBotStarRefModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val totalTransactions: Int
        get() = transactions.size

    val connectedBotsCount: Int
        get() = connectedBots.count { it.isActive }
}
