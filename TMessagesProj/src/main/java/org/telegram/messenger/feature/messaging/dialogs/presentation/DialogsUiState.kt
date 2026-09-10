package org.telegram.messenger.feature.messaging.dialogs.presentation

import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel

/**
 * UI State for the Dialogs list screen.
 */
sealed class DialogsUiState {
    data object Loading : DialogsUiState()

    data class Success(
        val dialogs: List<DialogModel>,
        val currentFolderId: Int = 0,
        val isLoadingMore: Boolean = false
    ) : DialogsUiState()

    data class Error(
        val message: String?
    ) : DialogsUiState()
}
