package org.telegram.messenger.feature.business.botstars.domain.model

data class StarRefProgramModel(
    val botId: Long,
    val commissionPermille: Int = 0,
    val durationMonths: Int = 0,
    val endDate: Int = 0,
    val dailyRevenuePerUser: Long = 0L
)
