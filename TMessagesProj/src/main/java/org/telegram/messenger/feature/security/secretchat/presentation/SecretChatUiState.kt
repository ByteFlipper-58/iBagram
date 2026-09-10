package org.telegram.messenger.feature.security.secretchat.presentation

import org.telegram.messenger.feature.security.secretchat.domain.model.SecretChatModel

/**
 * UI State for Secret Chat screen and lifecycle.
 */
sealed interface SecretChatUiState {
    object Loading : SecretChatUiState
    data class Success(val chat: SecretChatModel) : SecretChatUiState
    data class Error(val message: String?) : SecretChatUiState
}
