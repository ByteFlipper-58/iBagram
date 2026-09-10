package org.telegram.messenger.feature.appconfig.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.appconfig.domain.repository.AppConfigRepository
import org.telegram.messenger.feature.appconfig.domain.usecase.ObserveAppConfigUseCase
import org.telegram.messenger.feature.appconfig.domain.usecase.ReloadAppConfigUseCase
import org.telegram.messenger.feature.appconfig.domain.usecase.UpdateAppConfigValueUseCase

class AppConfigViewModel(
    private val observeAppConfig: ObserveAppConfigUseCase,
    private val reloadAppConfig: ReloadAppConfigUseCase,
    private val updateAppConfigValue: UpdateAppConfigValueUseCase,
    private val repository: AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppConfigUiState(config = repository.getConfig()))
    val uiState: StateFlow<AppConfigUiState> = _uiState.asStateFlow()

    init {
        observeAppConfig()
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        config = state,
                        isLoading = false,
                        error = null,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AppConfigEvent) {
        when (event) {
            is AppConfigEvent.Refresh -> {
                _uiState.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    val result = reloadAppConfig()
                    result.onFailure { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = error.message ?: "Failed to reload app config")
                        }
                    }
                }
            }
            is AppConfigEvent.UpdateKey -> {
                viewModelScope.launch {
                    updateAppConfigValue(event.key, event.value)
                }
            }
        }
    }
}
