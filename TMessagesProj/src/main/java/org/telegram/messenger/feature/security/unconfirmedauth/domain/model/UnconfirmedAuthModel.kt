package org.telegram.messenger.feature.security.unconfirmedauth.domain.model

data class UnconfirmedAuthModel(
    val hash: Long,
    val date: Int,
    val device: String,
    val location: String,
    val isBot: Boolean = false,
    val botId: Long = 0L,
    val expiresAfterSeconds: Long = 0L,
    val isExpired: Boolean = false,
)
