package org.telegram.messenger.feature.messaging.dialogs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.DeleteDialogUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.GetDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.LoadMoreDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.MarkDialogAsReadUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.PinDialogUseCase

/**
 * ViewModel managing UI state and operations for the dialogs list.
 */
class DialogsViewModel(
    val account: Int,
    private val getDialogsUseCase: GetDialogsUseCase,
    private val loadMoreDialogsUseCase: LoadMoreDialogsUseCase,
    private val pinDialogUseCase: PinDialogUseCase,
    private val deleteDialogUseCase: DeleteDialogUseCase,
    private val markDialogAsReadUseCase: MarkDialogAsReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DialogsUiState>(DialogsUiState.Loading)
    val uiState: StateFlow<DialogsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DialogsEvent>()
    val events: SharedFlow<DialogsEvent> = _events.asSharedFlow()

    private var currentFolderId: Int = 0
    private var observeJob: Job? = null

    init {
        observeDialogs(0)
    }

    fun switchFolder(folderId: Int) {
        if (currentFolderId == folderId && _uiState.value is DialogsUiState.Success) return
        currentFolderId = folderId
        observeDialogs(folderId)
    }

    private fun observeDialogs(folderId: Int) {
        observeJob?.cancel()
        _uiState.value = DialogsUiState.Loading

        observeJob = getDialogsUseCase(folderId)
            .onEach { list ->
                _uiState.value = DialogsUiState.Success(
                    dialogs = list,
                    currentFolderId = folderId,
                    isLoadingMore = false
                )
            }
            .catch { error ->
                _uiState.value = DialogsUiState.Error(error.message)
            }
            .launchIn(viewModelScope)
    }

    fun onLoadMore() {
        val currentState = _uiState.value
        if (currentState is DialogsUiState.Success && !currentState.isLoadingMore) {
            _uiState.value = currentState.copy(isLoadingMore = true)
            viewModelScope.launch {
                val result = loadMoreDialogsUseCase(currentFolderId)
                if (result is Result.Failure) {
                    _events.emit(DialogsEvent.ShowError(result.error.message))
                }
                val latest = _uiState.value
                if (latest is DialogsUiState.Success) {
                    _uiState.value = latest.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun onTogglePin(dialogId: Long, pin: Boolean) {
        viewModelScope.launch {
            val result = pinDialogUseCase(dialogId, pin)
            if (result is Result.Failure) {
                _events.emit(DialogsEvent.ShowError(result.error.message))
            }
        }
    }

    fun onDeleteDialog(dialogId: Long, revoke: Boolean = true) {
        viewModelScope.launch {
            val result = deleteDialogUseCase(dialogId, revoke)
            if (result is Result.Failure) {
                _events.emit(DialogsEvent.ShowError(result.error.message))
            }
        }
    }

    fun onMarkAsRead(dialogId: Long) {
        viewModelScope.launch {
            val result = markDialogAsReadUseCase(dialogId)
            if (result is Result.Failure) {
                _events.emit(DialogsEvent.ShowError(result.error.message))
            }
        }
    }
}
