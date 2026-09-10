package org.telegram.messenger.feature.system.hints.presentation

import org.telegram.messenger.feature.system.hints.domain.model.HintType

/**
 * MVI Events for HintsViewModel.
 */
sealed interface HintsEvent {
    object Refresh : HintsEvent
    data class CheckShouldShow(val type: HintType) : HintsEvent
    data class Increment(val type: HintType) : HintsEvent
    data class DoNotShowAgain(val type: HintType) : HintsEvent
    data class Reset(val type: HintType) : HintsEvent
    object ResetAll : HintsEvent
    object DismissInfo : HintsEvent
}
