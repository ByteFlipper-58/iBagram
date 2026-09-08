package org.telegram.messenger.feature.location.domain.model

/**
 * Domain model representing a peer's live location received in a dialog.
 */
data class PeerLiveLocationModel(
    val messageId: Int,
    val dialogId: Long,
    val fromId: Long,
    val geoPoint: GeoPointModel,
    val date: Long,
    val expiresIn: Int = 0,
    val unread: Boolean = false
)
