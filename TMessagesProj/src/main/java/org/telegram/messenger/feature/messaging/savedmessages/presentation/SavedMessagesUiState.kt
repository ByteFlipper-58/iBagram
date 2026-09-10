package org.telegram.messenger.feature.messaging.savedmessages.presentation

import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.model.SavedTagModel

/**
 * Immutable UI state for the Saved Messages screen.
 */
sealed class SavedMessagesUiState {
    data object Loading : SavedMessagesUiState()

    data class Content(
        val dialogs: List<SavedDialogModel>,
        val tags: List<SavedTagModel> = emptyList(),
        val searchQuery: String = "",
        val searchResults: List<SavedDialogModel>? = null
    ) : SavedMessagesUiState() {
        val displayedDialogs: List<SavedDialogModel>
            get() = searchResults ?: dialogs
    }

    data class Error(val message: String) : SavedMessagesUiState()
}
