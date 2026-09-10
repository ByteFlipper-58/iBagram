package org.telegram.messenger.feature.media.voip.domain.model

/**
 * Pure Kotlin immutable domain model representing an active or recent VoIP call.
 * Decouples presentation from [org.telegram.messenger.voip.VoIPService].
 */
data class CallModel(
    val userId: Long,
    val userName: String = "",
    val isOutgoing: Boolean = false,
    val isVideo: Boolean = false,
    val state: CallState = CallState.IDLE,
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerphoneOn: Boolean = false
)
