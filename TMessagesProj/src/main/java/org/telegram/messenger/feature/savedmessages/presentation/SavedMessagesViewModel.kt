package org.telegram.messenger.feature.savedmessages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.savedmessages.domain.model.SavedDialogModel
import org.telegram.messenger.feature.savedmessages.domain.usecase.DeleteSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedTagsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.SearchSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase

/**
 * ViewModel managing UI state and user intents for Saved Messages.
 */
class SavedMessagesViewModel(
    val account: Int,
    private val getSavedDialogsUseCase: GetSavedDialogsUseCase,
    private val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase,
    private val deleteSavedDialogUseCase: DeleteSavedDialogUseCase? = null,
    private val getSavedTagsUseCase: GetSavedTagsUseCase? = null,
    private val searchSavedDialogsUseCase: SearchSavedDialogsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedMessagesUiState>(SavedMessagesUiState.Loading)
    val uiState: StateFlow<SavedMessagesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SavedMessagesEvent>()
    val events: SharedFlow<SavedMessagesEvent> = _events.asSharedFlow()

    init {
        observeDialogs()
        observeTags()
    }

    private fun observeDialogs() {
        getSavedDialogsUseCase.observe()
            .onEach { dialogs ->
                val current = _uiState.value
                if (current is SavedMessagesUiState.Content) {
                    val updatedSearchResults = if (current.searchQuery.isNotEmpty()) {
                        searchSavedDialogsUseCase?.invoke(current.searchQuery)
                            ?: dialogs.filter { it.title.contains(current.searchQuery, ignoreCase = true) }
                    } else {
                        null
                    }
                    _uiState.value = current.copy(dialogs = dialogs, searchResults = updatedSearchResults)
                } else {
                    _uiState.value = SavedMessagesUiState.Content(dialogs = dialogs)
                }
            }
            .catch { error ->
                _uiState.value = SavedMessagesUiState.Error(error.message ?: "Unknown error")
            }
            .launchIn(viewModelScope)
    }

    private fun observeTags() {
        getSavedTagsUseCase?.observe()
            ?.onEach { tags ->
                val current = _uiState.value
                if (current is SavedMessagesUiState.Content) {
                    _uiState.value = current.copy(tags = tags)
                }
            }
            ?.launchIn(viewModelScope)
    }

    fun onSearch(query: String) {
        val current = _uiState.value
        if (current is SavedMessagesUiState.Content) {
            val trimmed = query.trim()
            val results = if (trimmed.isNotEmpty()) {
                searchSavedDialogsUseCase?.invoke(trimmed)
                    ?: current.dialogs.filter { it.title.contains(trimmed, ignoreCase = true) }
            } else {
                null
            }
            _uiState.value = current.copy(searchQuery = query, searchResults = results)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            getSavedDialogsUseCase.refresh()
        }
    }

    fun onDialogClicked(dialog: SavedDialogModel) {
        viewModelScope.launch {
            _events.emit(SavedMessagesEvent.NavigateToChat(dialog.dialogId))
        }
    }

    fun onTogglePin(dialog: SavedDialogModel) {
        viewModelScope.launch {
            val result = togglePinSavedDialogUseCase(dialog.dialogId, !dialog.isPinned)
            if (result is Result.Failure) {
                _events.emit(SavedMessagesEvent.ShowError(result.error.message))
            }
        }
    }

    fun onDeleteDialog(dialog: SavedDialogModel) {
        viewModelScope.launch {
            val result = deleteSavedDialogUseCase?.invoke(dialog.dialogId)
            if (result is Result.Failure) {
                _events.emit(SavedMessagesEvent.ShowError(result.error.message))
            }
        }
    }
}
