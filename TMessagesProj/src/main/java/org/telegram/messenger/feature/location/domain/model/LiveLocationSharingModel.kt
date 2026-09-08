package org.telegram.messenger.feature.location.domain.model

/**
 * Domain model representing an ongoing live location sharing session in a dialog.
 */
data class LiveLocationSharingModel(
    val dialogId: Long,
    val messageId: Int,
    val stopTime: Long,
    val period: Int,
    val proximityMeters: Int = 0,
    val isActive: Boolean = true
)
