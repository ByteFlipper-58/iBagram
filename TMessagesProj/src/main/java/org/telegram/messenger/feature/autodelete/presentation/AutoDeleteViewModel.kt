package org.telegram.messenger.feature.autodelete.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.autodelete.domain.usecase.GetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.GetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.ObserveGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatsAutoDeleteBatchUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetGlobalAutoDeleteUseCase

class AutoDeleteViewModel(
    private val observeGlobalAutoDeleteUseCase: ObserveGlobalAutoDeleteUseCase,
    private val getGlobalAutoDeleteUseCase: GetGlobalAutoDeleteUseCase,
    private val setGlobalAutoDeleteUseCase: SetGlobalAutoDeleteUseCase,
    private val getChatAutoDeleteUseCase: GetChatAutoDeleteUseCase,
    private val setChatAutoDeleteUseCase: SetChatAutoDeleteUseCase,
    private val setChatsAutoDeleteBatchUseCase: SetChatsAutoDeleteBatchUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutoDeleteUiState>(AutoDeleteUiState.Initial)
    val uiState: StateFlow<AutoDeleteUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadGlobalTtl()
    }

    fun onEvent(event: AutoDeleteEvent) {
        when (event) {
            is AutoDeleteEvent.LoadGlobalTtl -> loadGlobalTtl(event.forceRefresh)
            is AutoDeleteEvent.SetGlobalTtl -> setGlobalTtl(event.ttl)
            is AutoDeleteEvent.SetChatTtl -> setChatTtl(event.chatId, event.ttl)
            is AutoDeleteEvent.SetChatsTtlBatch -> setChatsTtlBatch(event.chatIds, event.ttl)
            is AutoDeleteEvent.ClearError -> clearError()
        }
    }

    fun loadGlobalTtl(forceRefresh: Boolean = false) {
        observeJob?.cancel()
        if (_uiState.value is AutoDeleteUiState.Initial) {
            _uiState.value = AutoDeleteUiState.Loading
        }

        viewModelScope.launch {
            observeJob = launch {
                observeGlobalAutoDeleteUseCase()
                    .catch { e ->
                        _uiState.value = AutoDeleteUiState.Error(e.message ?: "Failed to observe auto-delete TTL")
                    }
                    .collect { state ->
                        val current = _uiState.value
                        val isSaving = if (current is AutoDeleteUiState.Success) current.isSaving else false
                        val error = if (current is AutoDeleteUiState.Success) current.error else null

                        _uiState.value = AutoDeleteUiState.Success(
                            globalTtl = state.ttl,
                            isSaving = isSaving,
                            error = error
                        )
                    }
            }

            if (forceRefresh) {
                when (val result = getGlobalAutoDeleteUseCase(forceRefresh = true)) {
                    is Result.Success -> {
                        // Handled by observeGlobalAutoDeleteUseCase flow
                    }
                    is Result.Failure -> {
                        updateError(result.error.message)
                    }
                }
            }
        }
    }

    fun setGlobalTtl(ttl: AutoDeleteTtlModel) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = setGlobalAutoDeleteUseCase(ttl)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun setChatTtl(chatId: Long, ttl: AutoDeleteTtlModel) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = setChatAutoDeleteUseCase(chatId, ttl)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun setChatsTtlBatch(chatIds: List<Long>, ttl: AutoDeleteTtlModel) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = setChatsAutoDeleteBatchUseCase(chatIds, ttl)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is AutoDeleteUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun setSaving(isSaving: Boolean) {
        val current = _uiState.value
        if (current is AutoDeleteUiState.Success) {
            _uiState.value = current.copy(isSaving = isSaving)
        }
    }

    private fun updateError(error: String) {
        val current = _uiState.value
        if (current is AutoDeleteUiState.Success) {
            _uiState.value = current.copy(error = error, isSaving = false)
        } else {
            _uiState.value = AutoDeleteUiState.Error(error)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
