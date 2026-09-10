package org.telegram.messenger.feature.adjustpan.presentation

import org.telegram.messenger.feature.adjustpan.domain.model.PanCalculationSpec

sealed interface AdjustPanEvent {
    data class SetEnabled(val enabled: Boolean) : AdjustPanEvent
    data class PrepareTransition(val spec: PanCalculationSpec) : AdjustPanEvent
    data class UpdateProgress(val progress: Float) : AdjustPanEvent
    data class CompleteTransition(val progress: Float = 0f, val isKeyboardVisible: Boolean = false) : AdjustPanEvent
    object Reset : AdjustPanEvent
}
