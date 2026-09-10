package org.telegram.messenger.feature.security.privacy.domain.model

data class BlockedPeerModel(
    val peerId: Long,
    val isUser: Boolean = true,
    val date: Int = 0
)
