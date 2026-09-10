package org.telegram.messenger.feature.system.hints.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.hints.domain.model.HintType
import org.telegram.messenger.feature.system.hints.domain.usecase.DoNotShowAgainHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.GetHintsStateUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.IncrementHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ObserveHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetAllHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ShouldShowHintUseCase

/**
 * ViewModel managing user hints state and actions.
 */
class HintsViewModel(
    private val observeHintsUseCase: ObserveHintsUseCase,
    private val getHintsStateUseCase: GetHintsStateUseCase,
    private val shouldShowHintUseCase: ShouldShowHintUseCase,
    private val incrementHintUseCase: IncrementHintUseCase,
    private val doNotShowAgainHintUseCase: DoNotShowAgainHintUseCase,
    private val resetHintUseCase: ResetHintUseCase,
    private val resetAllHintsUseCase: ResetAllHintsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HintsUiState(hintsState = getHintsStateUseCase()))
    val uiState: StateFlow<HintsUiState> = _uiState.asStateFlow()

    init {
        observeHintsUseCase()
            .onEach { state ->
                _uiState.update { it.copy(hintsState = state, isRefreshing = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HintsEvent) {
        when (event) {
            is HintsEvent.Refresh -> refresh()
            is HintsEvent.CheckShouldShow -> checkShouldShow(event.type)
            is HintsEvent.Increment -> increment(event.type)
            is HintsEvent.DoNotShowAgain -> doNotShowAgain(event.type)
            is HintsEvent.Reset -> reset(event.type)
            is HintsEvent.ResetAll -> resetAll()
            is HintsEvent.DismissInfo -> dismissInfo()
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        val current = getHintsStateUseCase()
        _uiState.update { it.copy(hintsState = current, isRefreshing = false) }
    }

    private fun checkShouldShow(type: HintType) {
        viewModelScope.launch {
            val shouldShow = shouldShowHintUseCase(type)
            _uiState.update { it.copy(lastShownDecision = Pair(type, shouldShow)) }
        }
    }

    private fun increment(type: HintType) {
        viewModelScope.launch {
            incrementHintUseCase(type)
        }
    }

    private fun doNotShowAgain(type: HintType) {
        viewModelScope.launch {
            doNotShowAgainHintUseCase(type)
            _uiState.update { it.copy(infoMessage = "Hint ${type.name} marked as do not show again") }
        }
    }

    private fun reset(type: HintType) {
        viewModelScope.launch {
            resetHintUseCase(type)
            _uiState.update { it.copy(infoMessage = "Hint ${type.name} reset") }
        }
    }

    private fun resetAll() {
        viewModelScope.launch {
            resetAllHintsUseCase()
            _uiState.update { it.copy(infoMessage = "All hints reset") }
        }
    }

    private fun dismissInfo() {
        _uiState.update { it.copy(infoMessage = null) }
    }
}
