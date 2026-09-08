package org.telegram.messenger.feature.privacy.domain.model

data class BlockedPeerModel(
    val peerId: Long,
    val isUser: Boolean = true,
    val date: Int = 0
)
