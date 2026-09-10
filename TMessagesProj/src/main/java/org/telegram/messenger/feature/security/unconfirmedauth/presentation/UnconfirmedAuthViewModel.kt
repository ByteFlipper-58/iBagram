package org.telegram.messenger.feature.security.unconfirmedauth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ClearUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.GetUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ObserveUnconfirmedAuthsUseCase

class UnconfirmedAuthViewModel(
    private val observeUnconfirmedAuthsUseCase: ObserveUnconfirmedAuthsUseCase,
    private val getUnconfirmedAuthsUseCase: GetUnconfirmedAuthsUseCase,
    private val confirmAuthUseCase: ConfirmAuthUseCase,
    private val denyAuthUseCase: DenyAuthUseCase,
    private val confirmAllAuthsUseCase: ConfirmAllAuthsUseCase,
    private val denyAllAuthsUseCase: DenyAllAuthsUseCase,
    private val clearUnconfirmedAuthsUseCase: ClearUnconfirmedAuthsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnconfirmedAuthUiState>(UnconfirmedAuthUiState.Initial)
    val uiState: StateFlow<UnconfirmedAuthUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadAuths()
    }

    fun onEvent(event: UnconfirmedAuthEvent) {
        when (event) {
            is UnconfirmedAuthEvent.LoadAuths -> loadAuths()
            is UnconfirmedAuthEvent.ConfirmAuth -> confirmAuth(event.hash)
            is UnconfirmedAuthEvent.DenyAuth -> denyAuth(event.hash)
            is UnconfirmedAuthEvent.ConfirmAll -> confirmAll()
            is UnconfirmedAuthEvent.DenyAll -> denyAll()
            is UnconfirmedAuthEvent.ClearAll -> clearAll()
            is UnconfirmedAuthEvent.ClearError -> clearError()
        }
    }

    fun loadAuths() {
        observeJob?.cancel()
        if (_uiState.value is UnconfirmedAuthUiState.Initial) {
            _uiState.value = UnconfirmedAuthUiState.Loading
        }

        observeJob = viewModelScope.launch {
            observeUnconfirmedAuthsUseCase()
                .catch { e ->
                    _uiState.value = UnconfirmedAuthUiState.Error(e.message ?: "Failed to observe unconfirmed auths")
                }
                .collect { state ->
                    val currentState = _uiState.value
                    val isProcessing = (currentState as? UnconfirmedAuthUiState.Success)?.isProcessing ?: false
                    _uiState.value = UnconfirmedAuthUiState.Success(
                        auths = state.auths,
                        isProcessing = isProcessing
                    )
                }
        }
    }

    fun confirmAuth(hash: Long) {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = confirmAuthUseCase(hash)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun denyAuth(hash: Long) {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = denyAuthUseCase(hash)) {
                is Result.Success -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun confirmAll() {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = confirmAllAuthsUseCase()) {
                is Result.Success -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun denyAll() {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = denyAllAuthsUseCase()) {
                is Result.Success -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun clearAll() {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(isProcessing = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = clearUnconfirmedAuthsUseCase()) {
                is Result.Success -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(isProcessing = false)
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
                is Result.Failure -> {
                    val updated = (_uiState.value as? UnconfirmedAuthUiState.Success)?.copy(
                        isProcessing = false,
                        errorMessage = result.error.message
                    )
                    if (updated != null) {
                        _uiState.value = updated
                    }
                }
            }
        }
    }

    fun clearError() {
        val current = _uiState.value as? UnconfirmedAuthUiState.Success ?: return
        _uiState.value = current.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
