package org.telegram.messenger.feature.quickreplies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.usecase.CanAddNewQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.DeleteQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.LoadQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ObserveQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.RenameQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ReorderQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.SendQuickReplyUseCase

class QuickRepliesViewModel(
    private val observeQuickRepliesUseCase: ObserveQuickRepliesUseCase,
    private val loadQuickRepliesUseCase: LoadQuickRepliesUseCase,
    private val canAddNewQuickReplyUseCase: CanAddNewQuickReplyUseCase,
    private val renameQuickReplyUseCase: RenameQuickReplyUseCase,
    private val reorderQuickRepliesUseCase: ReorderQuickRepliesUseCase,
    private val deleteQuickRepliesUseCase: DeleteQuickRepliesUseCase,
    private val sendQuickReplyUseCase: SendQuickReplyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickRepliesUiState())
    val uiState: StateFlow<QuickRepliesUiState> = _uiState.asStateFlow()

    init {
        observeReplies()
        checkCanAddNew()
    }

    private fun observeReplies() {
        viewModelScope.launch {
            observeQuickRepliesUseCase().collect { replies ->
                _uiState.update { it.copy(replies = replies) }
                checkCanAddNew()
            }
        }
    }

    fun onEvent(event: QuickRepliesEvent) {
        when (event) {
            is QuickRepliesEvent.Load -> load(event.force)
            is QuickRepliesEvent.Rename -> rename(event.id, event.newName)
            is QuickRepliesEvent.Reorder -> reorder(event.ids)
            is QuickRepliesEvent.Delete -> delete(event.ids)
            is QuickRepliesEvent.Send -> send(event.dialogId, event.shortcutId)
            is QuickRepliesEvent.CheckCanAddNew -> checkCanAddNew()
            is QuickRepliesEvent.ClearMessages -> clearMessages()
        }
    }

    private fun load(force: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loadQuickRepliesUseCase(force)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    checkCanAddNew()
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun rename(id: Int, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = renameQuickReplyUseCase(id, newName)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, actionSuccessMessage = "Quick reply renamed")
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun reorder(ids: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = reorderQuickRepliesUseCase(ids)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun delete(ids: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = deleteQuickRepliesUseCase(ids)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, actionSuccessMessage = "Quick replies deleted")
                    }
                    checkCanAddNew()
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun send(dialogId: Long, shortcutId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = sendQuickReplyUseCase(dialogId, shortcutId)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, actionSuccessMessage = "Quick reply sent")
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun checkCanAddNew() {
        viewModelScope.launch {
            when (val result = canAddNewQuickReplyUseCase()) {
                is Result.Success -> _uiState.update { it.copy(canAddNew = result.data) }
                is Result.Failure -> {}
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
