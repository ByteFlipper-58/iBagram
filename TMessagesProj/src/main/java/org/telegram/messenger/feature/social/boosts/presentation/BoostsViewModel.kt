package org.telegram.messenger.feature.social.boosts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.boosts.domain.usecase.ApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.CheckCanApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetBoostsStatusUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetMyBoostsUseCase

class BoostsViewModel(
    private val getBoostsStatusUseCase: GetBoostsStatusUseCase,
    private val getMyBoostsUseCase: GetMyBoostsUseCase,
    private val checkCanApplyBoostUseCase: CheckCanApplyBoostUseCase,
    private val applyBoostUseCase: ApplyBoostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoostsUiState())
    val uiState: StateFlow<BoostsUiState> = _uiState.asStateFlow()

    fun onEvent(event: BoostsEvent) {
        when (event) {
            is BoostsEvent.LoadStatus -> loadStatus(event.dialogId)
            is BoostsEvent.LoadMyBoosts -> loadMyBoosts()
            is BoostsEvent.CheckCanApply -> checkCanApply(event.dialogId)
            is BoostsEvent.ApplyBoost -> applyBoost(event.dialogId, event.slots)
            is BoostsEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadStatus(dialogId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getBoostsStatusUseCase(dialogId)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, status = result.data)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun loadMyBoosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getMyBoostsUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, myBoosts = result.data)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun checkCanApply(dialogId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = checkCanApplyBoostUseCase(dialogId)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, canApply = result.data)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun applyBoost(dialogId: Long, slots: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, errorMessage = null, actionSuccessMessage = null) }
            when (val result = applyBoostUseCase(dialogId, slots)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            myBoosts = result.data,
                            actionSuccessMessage = "Boost applied successfully"
                        )
                    }
                    loadStatus(dialogId)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isApplying = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
