package org.telegram.messenger.feature.browser.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.browser.domain.model.BrowserSettingsModel
import org.telegram.messenger.feature.browser.domain.model.BrowserType
import org.telegram.messenger.feature.browser.domain.usecase.CheckUrlSafetyUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ManageBrowserHistoryUseCase
import org.telegram.messenger.feature.browser.domain.usecase.ObserveBrowserStateUseCase
import org.telegram.messenger.feature.browser.domain.usecase.OpenBrowserUrlUseCase
import org.telegram.messenger.feature.browser.domain.usecase.UpdateBrowserSettingsUseCase

class BrowserViewModel(
    private val observeBrowserState: ObserveBrowserStateUseCase,
    private val updateBrowserSettings: UpdateBrowserSettingsUseCase,
    private val checkUrlSafety: CheckUrlSafetyUseCase,
    private val openBrowserUrl: OpenBrowserUrlUseCase,
    private val manageBrowserHistory: ManageBrowserHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        observeBrowserState()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        settings = state.settings,
                        recentHistory = state.recentHistory,
                        lastOpenedUrl = state.lastOpenedUrl
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.SetBrowserType -> {
                viewModelScope.launch {
                    updateBrowserSettings.setBrowserType(event.type)
                }
            }
            is BrowserEvent.UpdateSettings -> {
                viewModelScope.launch {
                    updateBrowserSettings(event.settings)
                }
            }
            is BrowserEvent.ToggleWarnExternalLinks -> {
                viewModelScope.launch {
                    val currentSettings = _uiState.value.settings
                    updateBrowserSettings(currentSettings.copy(warnOnExternalLinks = event.enabled))
                }
            }
            is BrowserEvent.CheckUrl -> {
                val result = checkUrlSafety(event.url)
                _uiState.update { it.copy(currentUrlCheck = result) }
            }
            is BrowserEvent.OpenUrl -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        openBrowserUrl(event.url, event.forceExternal)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message ?: "Failed to open URL") }
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            is BrowserEvent.ClearHistory -> {
                viewModelScope.launch {
                    manageBrowserHistory.clearHistory()
                }
            }
            is BrowserEvent.ClearCacheAndCookies -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        manageBrowserHistory.clearCacheAndCookies()
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message ?: "Failed to clear cache") }
                    } finally {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
            is BrowserEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
