package org.telegram.messenger.feature.system.floatingdebug.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ClearFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.GetFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ObserveFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.RegisterFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.SetFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ToggleFloatingDebugActiveUseCase

class FloatingDebugViewModel(
    private val observeFloatingDebugStateUseCase: ObserveFloatingDebugStateUseCase,
    private val getFloatingDebugStateUseCase: GetFloatingDebugStateUseCase,
    private val setFloatingDebugActiveUseCase: SetFloatingDebugActiveUseCase,
    private val toggleFloatingDebugActiveUseCase: ToggleFloatingDebugActiveUseCase,
    private val registerFloatingDebugItemsUseCase: RegisterFloatingDebugItemsUseCase,
    private val clearFloatingDebugItemsUseCase: ClearFloatingDebugItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FloatingDebugUiState(state = getFloatingDebugStateUseCase())
    )
    val uiState: StateFlow<FloatingDebugUiState> = _uiState.asStateFlow()

    init {
        observeFloatingDebugStateUseCase()
            .onEach { state ->
                _uiState.value = _uiState.value.copy(state = state)
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: FloatingDebugEvent) {
        when (event) {
            is FloatingDebugEvent.SetActive -> {
                setFloatingDebugActiveUseCase(event.active, event.saveConfig)
            }
            is FloatingDebugEvent.ToggleActive -> {
                toggleFloatingDebugActiveUseCase(event.saveConfig)
            }
            is FloatingDebugEvent.RegisterItems -> {
                registerFloatingDebugItemsUseCase(event.items)
            }
            is FloatingDebugEvent.ClearItems -> {
                clearFloatingDebugItemsUseCase()
            }
            is FloatingDebugEvent.SetMenuOpen -> {
                _uiState.value = _uiState.value.copy(isMenuOpen = event.isOpen)
            }
        }
    }
}
