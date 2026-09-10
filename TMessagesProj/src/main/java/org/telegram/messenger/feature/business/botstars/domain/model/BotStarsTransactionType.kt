package org.telegram.messenger.feature.business.botstars.domain.model

enum class BotStarsTransactionType(val legacyId: Int) {
    ALL(0),
    INCOMING(1),
    OUTGOING(2);

    companion object {
        fun fromLegacyId(id: Int): BotStarsTransactionType = when (id) {
            1 -> INCOMING
            2 -> OUTGOING
            else -> ALL
        }
    }
}
