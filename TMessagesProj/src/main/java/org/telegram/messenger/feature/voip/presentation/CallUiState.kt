package org.telegram.messenger.feature.voip.presentation

import org.telegram.messenger.feature.voip.domain.model.CallModel

/**
 * Pure Kotlin UI state representing the VoIP call screen.
 */
sealed interface CallUiState {
    object Idle : CallUiState
    data class Active(val call: CallModel) : CallUiState
    data class Ended(val reason: String? = null) : CallUiState
}
