package org.telegram.messenger.feature.hints.presentation

import org.telegram.messenger.feature.hints.domain.model.HintType
import org.telegram.messenger.feature.hints.domain.model.HintsStateModel

/**
 * UI State for hints and discovery tips management.
 */
data class HintsUiState(
    val hintsState: HintsStateModel = HintsStateModel(),
    val isRefreshing: Boolean = false,
    val lastShownDecision: Pair<HintType, Boolean>? = null,
    val infoMessage: String? = null
) {
    val activeHintsCount: Int
        get() = hintsState.activeHintsCount
}
