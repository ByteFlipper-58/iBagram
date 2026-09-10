package org.telegram.messenger.feature.messaging.chat.presentation

import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel

/**
 * UI state for the chat screen.
 */
sealed class ChatUiState {
    data object Loading : ChatUiState()

    data class Success(
        val messages: List<MessageModel>,
        val dialogId: Long,
        val isSending: Boolean = false,
        val isLoadingHistory: Boolean = false
    ) : ChatUiState()

    data class Error(
        val message: String?
    ) : ChatUiState()
}
