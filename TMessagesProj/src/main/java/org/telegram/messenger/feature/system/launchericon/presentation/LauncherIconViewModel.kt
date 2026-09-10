package org.telegram.messenger.feature.system.launchericon.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.usecase.FixLauncherIconIfNeededUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetActiveLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.ObserveLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.SetLauncherIconUseCase

class LauncherIconViewModel(
    private val observeLauncherIconsUseCase: ObserveLauncherIconsUseCase,
    private val getLauncherIconsUseCase: GetLauncherIconsUseCase,
    private val getActiveLauncherIconUseCase: GetActiveLauncherIconUseCase,
    private val setLauncherIconUseCase: SetLauncherIconUseCase,
    private val fixLauncherIconIfNeededUseCase: FixLauncherIconIfNeededUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LauncherIconUiState(
            icons = getLauncherIconsUseCase(),
            activeIcon = getActiveLauncherIconUseCase()
        )
    )
    val uiState: StateFlow<LauncherIconUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeLauncherIconsUseCase().collect { state ->
                _uiState.update { current ->
                    current.copy(
                        icons = state.icons,
                        activeIcon = state.activeIcon
                    )
                }
            }
        }
    }

    fun onEvent(event: LauncherIconEvent) {
        when (event) {
            is LauncherIconEvent.SelectIcon -> selectIcon(event.type)
            is LauncherIconEvent.FixIconIfNeeded -> fixIconIfNeeded()
            is LauncherIconEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun selectIcon(type: LauncherIconType) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = setLauncherIconUseCase(type)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun fixIconIfNeeded() {
        viewModelScope.launch {
            when (val result = fixLauncherIconIfNeededUseCase()) {
                is Result.Success -> {
                    // Successfully checked and fixed if needed
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }
}
