package org.telegram.messenger.feature.windowvisibility.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.windowvisibility.domain.usecase.CheckIsWindowVisibleUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.GetActiveHideReasonsUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.GetWindowVisibilityStateUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.ObserveWindowVisibilityStateUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.ReleaseHideWindowUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.RequestHideWindowUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.ResetWindowVisibilityUseCase
import org.telegram.messenger.feature.windowvisibility.domain.usecase.ToggleWindowHideUseCase

class WindowVisibilityViewModel(
    private val requestHideWindowUseCase: RequestHideWindowUseCase,
    private val releaseHideWindowUseCase: ReleaseHideWindowUseCase,
    private val toggleWindowHideUseCase: ToggleWindowHideUseCase,
    private val checkIsWindowVisibleUseCase: CheckIsWindowVisibleUseCase,
    private val getWindowVisibilityStateUseCase: GetWindowVisibilityStateUseCase,
    private val getActiveHideReasonsUseCase: GetActiveHideReasonsUseCase,
    private val resetWindowVisibilityUseCase: ResetWindowVisibilityUseCase,
    private val observeWindowVisibilityStateUseCase: ObserveWindowVisibilityStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val snapshot = getWindowVisibilityStateUseCase()
            WindowVisibilityUiState(
                isVisible = snapshot.isVisible,
                isHidden = snapshot.isHidden,
                reasonsCount = snapshot.reasonsCount,
                activeReasons = snapshot.activeReasons,
                lastChangedReason = snapshot.lastChangedReason
            )
        }
    )
    val uiState: StateFlow<WindowVisibilityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeWindowVisibilityStateUseCase().collectLatest { state ->
                _uiState.update { current ->
                    current.copy(
                        isVisible = state.isVisible,
                        isHidden = state.isHidden,
                        reasonsCount = state.reasonsCount,
                        activeReasons = state.activeReasons,
                        lastChangedReason = state.lastChangedReason
                    )
                }
            }
        }
    }

    fun onEvent(event: WindowVisibilityEvent) {
        when (event) {
            is WindowVisibilityEvent.HideRequested -> {
                requestHideWindowUseCase(event.reasonTag, event.description)
            }
            is WindowVisibilityEvent.ReleaseRequested -> {
                releaseHideWindowUseCase(event.reasonTag)
            }
            is WindowVisibilityEvent.ToggleHide -> {
                toggleWindowHideUseCase(event.reasonTag, event.hide, event.description)
            }
            is WindowVisibilityEvent.ResetAll -> {
                resetWindowVisibilityUseCase()
            }
            is WindowVisibilityEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
