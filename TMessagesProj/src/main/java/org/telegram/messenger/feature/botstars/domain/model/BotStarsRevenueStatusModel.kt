package org.telegram.messenger.feature.botstars.domain.model

data class BotStarsRevenueStatusModel(
    val currentBalance: Long = 0L,
    val availableBalance: Long = 0L,
    val overallRevenue: Long = 0L,
    val withdrawalEnabled: Boolean = false,
    val nextWithdrawalAt: Long = 0L,
    val isTon: Boolean = false
) {
    val hasBalance: Boolean
        get() = currentBalance > 0 || availableBalance > 0 || overallRevenue > 0
}
