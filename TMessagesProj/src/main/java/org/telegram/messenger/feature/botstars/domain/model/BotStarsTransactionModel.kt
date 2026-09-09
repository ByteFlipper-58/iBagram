package org.telegram.messenger.feature.botstars.domain.model

data class BotStarsTransactionModel(
    val id: String,
    val stars: Long,
    val date: Long,
    val peerId: Long,
    val title: String? = null,
    val description: String? = null,
    val isOutgoing: Boolean = false,
    val isRefund: Boolean = false,
    val isPending: Boolean = false,
    val floodskip: Boolean = false,
    val starRefCommissionPermille: Int = 0
) {
    val isIncoming: Boolean
        get() = !isOutgoing
}
