package org.telegram.messenger.feature.media.autodeletemedia.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.media.autodeletemedia.domain.repository.AutoDeleteMediaRepository
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.LockFileUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.ObserveAutoDeleteStateUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.RunAutoDeleteCleanupUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.UnlockFileUseCase

class AutoDeleteMediaViewModel(
    private val observeAutoDeleteState: ObserveAutoDeleteStateUseCase,
    private val runAutoDeleteCleanup: RunAutoDeleteCleanupUseCase,
    private val lockFile: LockFileUseCase,
    private val unlockFile: UnlockFileUseCase,
    private val repository: AutoDeleteMediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutoDeleteMediaUiState(taskState = repository.getState()))
    val uiState: StateFlow<AutoDeleteMediaUiState> = _uiState.asStateFlow()

    init {
        observeAutoDeleteState()
            .onEach { taskState ->
                _uiState.update {
                    it.copy(
                        taskState = taskState,
                        lastResult = taskState.lastResult ?: it.lastResult
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AutoDeleteMediaEvent) {
        when (event) {
            is AutoDeleteMediaEvent.RunCleanup -> {
                _uiState.update { it.copy(isCleaningUp = true, errorMessage = null) }
                viewModelScope.launch {
                    val result = runAutoDeleteCleanup(event.force)
                    result.fold(
                        onSuccess = { res ->
                            _uiState.update {
                                it.copy(isCleaningUp = false, lastResult = res)
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(isCleaningUp = false, errorMessage = err.message ?: "Cleanup failed")
                            }
                        }
                    )
                }
            }
            is AutoDeleteMediaEvent.LockFile -> {
                lockFile(event.path)
            }
            is AutoDeleteMediaEvent.UnlockFile -> {
                unlockFile(event.path)
            }
            is AutoDeleteMediaEvent.ClearLockedFiles -> {
                repository.clearLockedFiles()
            }
            is AutoDeleteMediaEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
