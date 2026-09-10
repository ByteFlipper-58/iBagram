package org.telegram.messenger.feature.media.voip.domain.model

/**
 * Pure Kotlin typed enum representing VoIP call connection and lifecycle states.
 * Decouples presentation and domain layers from legacy VoIPService state ints.
 */
enum class CallState {
    IDLE,
    REQUESTING,
    WAITING_INCOMING,
    RINGING,
    CONNECTING,
    EXCHANGING_KEYS,
    ACTIVE,
    RECONNECTING,
    BUSY,
    ENDED,
    FAILED
}
