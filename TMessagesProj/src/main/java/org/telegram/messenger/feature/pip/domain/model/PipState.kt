package org.telegram.messenger.feature.pip.domain.model

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
