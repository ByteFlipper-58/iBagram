package org.telegram.messenger.feature.adjustpan.presentation

import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionPlan
import org.telegram.messenger.feature.adjustpan.domain.model.PanTransitionState

data class AdjustPanUiState(
    val transitionState: PanTransitionState = PanTransitionState(),
    val activePlan: PanTransitionPlan = PanTransitionPlan.NO_ANIMATION
)
