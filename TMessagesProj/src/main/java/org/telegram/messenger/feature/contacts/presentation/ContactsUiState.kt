package org.telegram.messenger.feature.contacts.presentation

import org.telegram.messenger.feature.contacts.domain.model.ContactModel

/**
 * Pure Kotlin UI state for the contacts list screen.
 */
sealed interface ContactsUiState {
    object Loading : ContactsUiState

    data class Success(
        val contacts: List<ContactModel>,
        val searchQuery: String = "",
        val searchResults: List<ContactModel> = emptyList()
    ) : ContactsUiState {
        val displayedContacts: List<ContactModel>
            get() = if (searchQuery.isBlank()) contacts else searchResults
    }

    data class Error(val message: String?) : ContactsUiState
}
