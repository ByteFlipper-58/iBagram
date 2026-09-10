package org.telegram.messenger.feature.security.passkeys.presentation

import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeyModel

sealed interface PasskeysUiState {
    data object Initial : PasskeysUiState
    data object Loading : PasskeysUiState
    data class Success(
        val passkeys: List<PasskeyModel> = emptyList(),
        val canAddPasskey: Boolean = true,
        val maxPasskeys: Int = 10,
        val isSupported: Boolean = true,
        val deletingPasskeyId: String? = null,
        val error: String? = null
    ) : PasskeysUiState
    data class Error(val message: String) : PasskeysUiState
}
