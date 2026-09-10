package org.telegram.messenger.feature.litemode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.litemode.domain.repository.LiteModeRepository
import org.telegram.messenger.feature.litemode.domain.usecase.ObserveLiteModeStateUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.SetLiteModePresetUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.ToggleLiteModeFlagUseCase
import org.telegram.messenger.feature.litemode.domain.usecase.UpdatePowerSaverThresholdUseCase

class LiteModeViewModel(
    private val observeLiteModeState: ObserveLiteModeStateUseCase,
    private val toggleLiteModeFlag: ToggleLiteModeFlagUseCase,
    private val setLiteModePreset: SetLiteModePresetUseCase,
    private val updatePowerSaverThreshold: UpdatePowerSaverThresholdUseCase,
    private val repository: LiteModeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiteModeUiState())
    val uiState: StateFlow<LiteModeUiState> = _uiState.asStateFlow()

    init {
        observeLiteModeState()
            .onEach { state ->
                _uiState.update { it.copy(state = state) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: LiteModeEvent) {
        when (event) {
            is LiteModeEvent.SetFlag -> {
                viewModelScope.launch {
                    toggleLiteModeFlag(event.flag, event.enabled)
                }
            }
            is LiteModeEvent.ToggleFlag -> {
                viewModelScope.launch {
                    toggleLiteModeFlag.toggle(event.flag)
                }
            }
            is LiteModeEvent.ApplyPreset -> {
                viewModelScope.launch {
                    setLiteModePreset(event.preset)
                }
            }
            is LiteModeEvent.SetPowerSaverThreshold -> {
                viewModelScope.launch {
                    updatePowerSaverThreshold(event.percentage)
                }
            }
            is LiteModeEvent.BatteryLevelChanged -> {
                viewModelScope.launch {
                    repository.updateBatteryLevel(event.level)
                }
            }
            is LiteModeEvent.PremiumChanged -> {
                viewModelScope.launch {
                    repository.setHasPremium(event.hasPremium)
                }
            }
            is LiteModeEvent.Reload -> {
                viewModelScope.launch {
                    repository.reload()
                }
            }
            is LiteModeEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
