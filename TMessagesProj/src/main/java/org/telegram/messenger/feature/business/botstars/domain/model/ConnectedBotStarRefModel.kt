package org.telegram.messenger.feature.business.botstars.domain.model

data class ConnectedBotStarRefModel(
    val botId: Long,
    val date: Int = 0,
    val url: String? = null,
    val commissionPermille: Int = 0,
    val durationMonths: Int = 0,
    val revoked: Boolean = false,
    val participants: Long = 0L,
    val revenue: Long = 0L
) {
    val isActive: Boolean
        get() = !revoked
}
