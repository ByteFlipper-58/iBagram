package org.telegram.messenger.feature.media.pip.presentation

import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState

/**
 * UI State representing current Picture-in-Picture status, active source details,
 * and media session controls.
 */
data class PipUiState(
    val isLoading: Boolean = false,
    val pipState: PipState = PipState.IDLE,
    val activeSource: PipSourceModel? = null,
    val registeredSources: List<PipSourceModel> = emptyList(),
    val registeredSourceCount: Int = 0,
    val canEnterPip: Boolean = false,
    val isMediaSessionActive: Boolean = false,
    val lastAction: Pair<String, Int>? = null,
    val errorMessage: String? = null
) {
    val activeSourceTag: String?
        get() = activeSource?.tag

    val activeSourcePriority: Int
        get() = activeSource?.priority ?: 0

    val isInPip: Boolean
        get() = pipState == PipState.IN_PIP || pipState == PipState.STASHED
}
