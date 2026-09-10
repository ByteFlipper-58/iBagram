package org.telegram.messenger.feature.system.adjustpan.presentation

import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.system.adjustpan.domain.model.PanTransitionState

data class AdjustPanUiState(
    val transitionState: PanTransitionState = PanTransitionState(),
    val activePlan: PanTransitionPlan = PanTransitionPlan.NO_ANIMATION
)
