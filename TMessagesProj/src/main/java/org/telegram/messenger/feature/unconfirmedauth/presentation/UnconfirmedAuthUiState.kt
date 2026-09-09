package org.telegram.messenger.feature.unconfirmedauth.presentation

import org.telegram.messenger.feature.unconfirmedauth.domain.model.UnconfirmedAuthModel

sealed interface UnconfirmedAuthUiState {
    object Initial : UnconfirmedAuthUiState
    object Loading : UnconfirmedAuthUiState
    data class Success(
        val auths: List<UnconfirmedAuthModel> = emptyList(),
        val isProcessing: Boolean = false,
        val errorMessage: String? = null
    ) : UnconfirmedAuthUiState {
        val hasPendingAuths: Boolean get() = auths.isNotEmpty()
    }
    data class Error(val message: String) : UnconfirmedAuthUiState
}
