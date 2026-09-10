package org.telegram.messenger.feature.media.voip.presentation

/**
 * Single-shot UI events emitted by [CallViewModel].
 */
sealed interface CallEvent {
    object CallAccepted : CallEvent
    object CallDeclined : CallEvent
    object CallEnded : CallEvent
    data class MuteToggled(val isMuted: Boolean) : CallEvent
    data class SpeakerphoneToggled(val isOn: Boolean) : CallEvent
    data class ShowError(val message: String) : CallEvent
}
