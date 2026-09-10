package org.telegram.messenger.feature.social.contacts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.domain.usecase.AddContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.DeleteContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.ObserveContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.SearchContactsUseCase

/**
 * ViewModel managing contacts list state, searching, and contact operations.
 */
class ContactsViewModel(
    private val observeContactsUseCase: ObserveContactsUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactUseCase: GetContactUseCase,
    private val addContactUseCase: AddContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
    private val searchContactsUseCase: SearchContactsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ContactsEvent>(Channel.BUFFERED)
    val events: Flow<ContactsEvent> = _events.receiveAsFlow()

    init {
        observeContacts()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            observeContactsUseCase().collect { contacts ->
                val current = _uiState.value
                if (current is ContactsUiState.Success && current.searchQuery.isNotBlank()) {
                    val filtered = searchContactsUseCase(current.searchQuery)
                    _uiState.value = current.copy(
                        contacts = contacts,
                        searchResults = filtered
                    )
                } else {
                    _uiState.value = ContactsUiState.Success(
                        contacts = contacts,
                        searchQuery = "",
                        searchResults = emptyList()
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val contacts = getContactsUseCase(forceReload = true)
                val current = _uiState.value
                if (current is ContactsUiState.Success && current.searchQuery.isNotBlank()) {
                    val filtered = searchContactsUseCase(current.searchQuery)
                    _uiState.value = current.copy(contacts = contacts, searchResults = filtered)
                } else {
                    _uiState.value = ContactsUiState.Success(contacts = contacts)
                }
            } catch (e: Throwable) {
                _uiState.value = ContactsUiState.Error(e.message)
            }
        }
    }

    fun search(query: String) {
        val current = _uiState.value
        if (current is ContactsUiState.Success) {
            viewModelScope.launch {
                if (query.isBlank()) {
                    _uiState.value = current.copy(searchQuery = "", searchResults = emptyList())
                } else {
                    val filtered = searchContactsUseCase(query)
                    _uiState.value = current.copy(searchQuery = query, searchResults = filtered)
                }
            }
        }
    }

    fun addContact(userId: Long, firstName: String, lastName: String, phone: String) {
        viewModelScope.launch {
            when (val result = addContactUseCase(userId, firstName, lastName, phone)) {
                is Result.Success -> _events.send(ContactsEvent.ContactAdded(userId))
                is Result.Failure -> _events.send(ContactsEvent.ShowError(result.error.message))
            }
        }
    }

    fun deleteContact(userId: Long) {
        viewModelScope.launch {
            when (val result = deleteContactUseCase(userId)) {
                is Result.Success -> _events.send(ContactsEvent.ContactDeleted(userId))
                is Result.Failure -> _events.send(ContactsEvent.ShowError(result.error.message))
            }
        }
    }
}
