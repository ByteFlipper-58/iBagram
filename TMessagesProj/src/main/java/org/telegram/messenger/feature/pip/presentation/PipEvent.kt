package org.telegram.messenger.feature.pip.presentation

import org.telegram.messenger.feature.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.pip.domain.model.PipState

/**
 * UI Events dispatched to [PipViewModel] to manage Picture-in-Picture sources,
 * lifecycle transitions, and remote action clicks.
 */
sealed class PipEvent {
    data class RegisterSource(val source: PipSourceModel) : PipEvent()
    data class UnregisterSource(val tag: String) : PipEvent()
    data class SetSourceAvailability(val tag: String, val isAvailable: Boolean) : PipEvent()
    data class SetSourceRatio(val tag: String, val width: Int, val height: Int) : PipEvent()
    data class SetSourceAttached(val tag: String, val isAttached: Boolean) : PipEvent()
    data class TransitionPipState(val state: PipState, val byActivityStop: Boolean = false) : PipEvent()
    data class TriggerAction(val tag: String, val actionId: Int) : PipEvent()
    object Refresh : PipEvent()
}
