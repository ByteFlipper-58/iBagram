package org.telegram.messenger.feature.security.flagsecure.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.AttachSecurityReasonUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.DetachSecurityReasonUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetWindowSecurityStateUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.InvalidateWindowSecurityUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveAllWindowStatesUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveWindowStateUseCase
import org.telegram.messenger.feature.security.flagsecure.domain.usecase.ResetWindowSecurityUseCase

class FlagSecureViewModel(
    private val initialWindowId: String = "main",
    private val attachSecurityReasonUseCase: AttachSecurityReasonUseCase,
    private val detachSecurityReasonUseCase: DetachSecurityReasonUseCase,
    private val invalidateWindowSecurityUseCase: InvalidateWindowSecurityUseCase,
    private val getWindowSecurityStateUseCase: GetWindowSecurityStateUseCase,
    private val resetWindowSecurityUseCase: ResetWindowSecurityUseCase,
    private val observeWindowStateUseCase: ObserveWindowStateUseCase,
    private val observeAllWindowStatesUseCase: ObserveAllWindowStatesUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow(
        FlagSecureUiState(
            currentWindowId = initialWindowId,
            windowState = getWindowSecurityStateUseCase(initialWindowId)
        )
    )
    val uiState: StateFlow<FlagSecureUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeWindowStateUseCase(initialWindowId).collect { state ->
                _uiState.update { it.copy(windowState = state) }
            }
        }
        scope.launch {
            observeAllWindowStatesUseCase().collect { allStates ->
                _uiState.update { it.copy(allWindows = allStates) }
            }
        }
    }

    fun onEvent(event: FlagSecureEvent) {
        when (event) {
            is FlagSecureEvent.SelectWindow -> selectWindow(event.windowId)
            is FlagSecureEvent.AttachReason -> attachReason(event.windowId, event.reason)
            is FlagSecureEvent.DetachReason -> detachReason(event.windowId, event.reason)
            is FlagSecureEvent.InvalidateWindow -> invalidateWindow(event.windowId)
            is FlagSecureEvent.ResetWindow -> resetWindow(event.windowId)
            is FlagSecureEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun selectWindow(windowId: String) {
        val state = getWindowSecurityStateUseCase(windowId)
        _uiState.update {
            it.copy(
                currentWindowId = windowId,
                windowState = state
            )
        }
    }

    private fun attachReason(windowId: String, reason: SecurityReasonType) {
        val newState = attachSecurityReasonUseCase(windowId, reason)
        if (windowId == _uiState.value.currentWindowId) {
            _uiState.update { it.copy(windowState = newState) }
        }
    }

    private fun detachReason(windowId: String, reason: SecurityReasonType) {
        val newState = detachSecurityReasonUseCase(windowId, reason)
        if (windowId == _uiState.value.currentWindowId) {
            _uiState.update { it.copy(windowState = newState) }
        }
    }

    private fun invalidateWindow(windowId: String) {
        val newState = invalidateWindowSecurityUseCase(windowId)
        if (windowId == _uiState.value.currentWindowId) {
            _uiState.update { it.copy(windowState = newState) }
        }
    }

    private fun resetWindow(windowId: String) {
        resetWindowSecurityUseCase(windowId)
        if (windowId == _uiState.value.currentWindowId) {
            _uiState.update { it.copy(windowState = getWindowSecurityStateUseCase(windowId)) }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
