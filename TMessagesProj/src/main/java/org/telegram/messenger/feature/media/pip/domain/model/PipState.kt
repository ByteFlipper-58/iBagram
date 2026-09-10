package org.telegram.messenger.feature.media.pip.domain.model

/**
 * Lifecycle state of the Picture-in-Picture window.
 */
enum class PipState {
    IDLE,
    ENTERING,
    IN_PIP,
    STASHED,
    EXITING
}
