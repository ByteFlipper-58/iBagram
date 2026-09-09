package org.telegram.messenger.feature.botstars.domain.model

data class StarRefProgramModel(
    val botId: Long,
    val commissionPermille: Int = 0,
    val durationMonths: Int = 0,
    val endDate: Int = 0,
    val dailyRevenuePerUser: Long = 0L
)
