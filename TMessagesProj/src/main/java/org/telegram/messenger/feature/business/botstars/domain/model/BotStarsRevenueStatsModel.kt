package org.telegram.messenger.feature.business.botstars.domain.model

data class BotStarsRevenueStatsModel(
    val dialogId: Long,
    val status: BotStarsRevenueStatusModel? = null,
    val usdRate: Double = 0.0
) {
    val currentBalance: Long
        get() = status?.currentBalance ?: 0L

    val availableBalance: Long
        get() = status?.availableBalance ?: 0L

    val overallRevenue: Long
        get() = status?.overallRevenue ?: 0L

    val isWithdrawalEnabled: Boolean
        get() = status?.withdrawalEnabled == true

    val hasStars: Boolean
        get() = status?.hasBalance == true
}
