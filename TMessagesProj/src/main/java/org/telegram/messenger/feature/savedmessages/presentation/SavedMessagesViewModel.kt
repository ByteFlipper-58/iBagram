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
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase

/**
 * ViewModel managing UI state and user intents for Saved Messages.
 */
class SavedMessagesViewModel(
    val account: Int,
    private val getSavedDialogsUseCase: GetSavedDialogsUseCase,
    private val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedMessagesUiState>(SavedMessagesUiState.Loading)
    val uiState: StateFlow<SavedMessagesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SavedMessagesEvent>()
    val events: SharedFlow<SavedMessagesEvent> = _events.asSharedFlow()

    init {
        observeDialogs()
    }

    private fun observeDialogs() {
        getSavedDialogsUseCase.observe()
            .onEach { dialogs ->
                val current = _uiState.value
                if (current is SavedMessagesUiState.Content && current.searchQuery.isNotEmpty()) {
                    _uiState.value = current.copy(dialogs = dialogs)
                } else {
                    _uiState.value = SavedMessagesUiState.Content(dialogs = dialogs)
                }
            }
            .catch { error ->
                _uiState.value = SavedMessagesUiState.Error(error.message ?: "Unknown error")
            }
            .launchIn(viewModelScope)
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
}
