package org.telegram.messenger.feature.security.passkeys.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.passkeys.domain.usecase.CheckCanAddPasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.DeletePasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.GetPasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.IsPasskeysSupportedUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.ObservePasskeysUseCase

class PasskeysViewModel(
    private val observePasskeysUseCase: ObservePasskeysUseCase,
    private val getPasskeysUseCase: GetPasskeysUseCase,
    private val deletePasskeyUseCase: DeletePasskeyUseCase,
    private val checkCanAddPasskeyUseCase: CheckCanAddPasskeyUseCase,
    private val isPasskeysSupportedUseCase: IsPasskeysSupportedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PasskeysUiState>(PasskeysUiState.Initial)
    val uiState: StateFlow<PasskeysUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun onEvent(event: PasskeysEvent) {
        when (event) {
            is PasskeysEvent.LoadPasskeys -> loadPasskeys(event.force)
            is PasskeysEvent.DeletePasskey -> deletePasskey(event.id)
            is PasskeysEvent.ClearError -> clearError()
        }
    }

    fun loadPasskeys(force: Boolean = false) {
        observeJob?.cancel()
        if (_uiState.value is PasskeysUiState.Initial) {
            _uiState.value = PasskeysUiState.Loading
        }

        viewModelScope.launch {
            observeJob = launch {
                observePasskeysUseCase()
                    .catch { e ->
                        _uiState.value = PasskeysUiState.Error(e.message ?: "Failed to observe passkeys")
                    }
                    .collect { state ->
                        val current = _uiState.value
                        val error = if (current is PasskeysUiState.Success) current.error else null
                        val deletingId = if (current is PasskeysUiState.Success) current.deletingPasskeyId else null

                        _uiState.value = PasskeysUiState.Success(
                            passkeys = state.passkeys,
                            canAddPasskey = state.canAddPasskey,
                            maxPasskeys = state.maxPasskeys,
                            isSupported = state.isSupported,
                            deletingPasskeyId = deletingId,
                            error = error
                        )
                    }
            }

            val result = getPasskeysUseCase(force)
            if (result is Result.Failure) {
                val current = _uiState.value
                if (current is PasskeysUiState.Success) {
                    _uiState.value = current.copy(error = result.error.message)
                } else {
                    _uiState.value = PasskeysUiState.Error(result.error.message)
                }
            }
        }
    }

    fun deletePasskey(id: String) {
        val current = _uiState.value
        if (current is PasskeysUiState.Success) {
            _uiState.value = current.copy(deletingPasskeyId = id)
        }

        viewModelScope.launch {
            val result = deletePasskeyUseCase(id)
            val updated = _uiState.value
            if (updated is PasskeysUiState.Success) {
                when (result) {
                    is Result.Success -> {
                        _uiState.value = updated.copy(
                            deletingPasskeyId = null,
                            error = null
                        )
                    }
                    is Result.Failure -> {
                        _uiState.value = updated.copy(
                            deletingPasskeyId = null,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is PasskeysUiState.Success) {
            _uiState.value = current.copy(error = null)
        }
    }
}
