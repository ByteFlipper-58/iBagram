package org.telegram.messenger.feature.themes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.themes.domain.repository.ThemeRepository
import org.telegram.messenger.feature.themes.domain.usecase.ApplyThemeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.GetAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.GetAvailableThemesUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveAvailableThemesUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveNightModeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ResetAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetBubbleRadiusUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetNightModeSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetNightModeTypeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetThemeAccentUseCase

/**
 * ViewModel governing Telegram themes, night mode, accents, and appearance.
 */
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase,
    private val getAppearanceSettingsUseCase: GetAppearanceSettingsUseCase,
    private val observeAvailableThemesUseCase: ObserveAvailableThemesUseCase,
    private val getAvailableThemesUseCase: GetAvailableThemesUseCase,
    private val applyThemeUseCase: ApplyThemeUseCase,
    private val observeNightModeUseCase: ObserveNightModeUseCase,
    private val setNightModeTypeUseCase: SetNightModeTypeUseCase,
    private val setNightModeSettingsUseCase: SetNightModeSettingsUseCase,
    private val setThemeAccentUseCase: SetThemeAccentUseCase,
    private val setBubbleRadiusUseCase: SetBubbleRadiusUseCase,
    private val resetAppearanceSettingsUseCase: ResetAppearanceSettingsUseCase
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(ThemeUiState())
    val uiState: StateFlow<ThemeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadData()
        observeData()
    }

    fun onEvent(event: ThemeEvent) {
        when (event) {
            is ThemeEvent.Load -> loadData()
            is ThemeEvent.SelectTheme -> selectTheme(event.themeKey, event.nightTheme)
            is ThemeEvent.SelectThemeAccent -> selectThemeAccent(event.themeKey, event.accentId)
            is ThemeEvent.SetNightMode -> setNightMode(event.type)
            is ThemeEvent.UpdateNightModeSettings -> updateNightModeSettings(event.settings)
            is ThemeEvent.SetBubbleRadius -> setBubbleRadius(event.radius)
            is ThemeEvent.ResetToDefaults -> resetToDefaults()
            is ThemeEvent.ClearMessage -> clearMessage()
        }
    }

    private fun loadData() {
        val settings = getAppearanceSettingsUseCase()
        val themes = getAvailableThemesUseCase()
        _uiState.update {
            it.copy(
                currentTheme = settings.currentTheme,
                currentNightTheme = settings.currentNightTheme,
                isNightModeActive = settings.isNightModeActive,
                nightModeSettings = settings.nightModeSettings,
                bubbleRadius = settings.bubbleRadius,
                wallpaper = settings.wallpaper,
                availableThemes = themes
            )
        }
    }

    private fun observeData() {
        scope.launch {
            observeAppearanceSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        currentTheme = settings.currentTheme,
                        currentNightTheme = settings.currentNightTheme,
                        isNightModeActive = settings.isNightModeActive,
                        nightModeSettings = settings.nightModeSettings,
                        bubbleRadius = settings.bubbleRadius,
                        wallpaper = settings.wallpaper
                    )
                }
            }
        }

        scope.launch {
            observeAvailableThemesUseCase().collect { themes ->
                _uiState.update {
                    it.copy(availableThemes = themes)
                }
            }
        }
    }

    private fun selectTheme(themeKey: String, nightTheme: Boolean) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = applyThemeUseCase(themeKey, nightTheme)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, userMessage = "Theme applied") }
                    _events.emit("ThemeApplied")
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun selectThemeAccent(themeKey: String, accentId: Int) {
        scope.launch {
            when (val res = setThemeAccentUseCase(themeKey, accentId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(userMessage = "Accent updated") }
                    _events.emit("AccentUpdated")
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun setNightMode(type: org.telegram.messenger.feature.themes.domain.model.NightModeType) {
        scope.launch {
            when (val res = setNightModeTypeUseCase(type)) {
                is Result.Success -> {
                    _uiState.update { it.copy(userMessage = "Night mode updated") }
                    _events.emit("NightModeUpdated")
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun updateNightModeSettings(settings: org.telegram.messenger.feature.themes.domain.model.NightModeSettingsModel) {
        scope.launch {
            when (val res = setNightModeSettingsUseCase(settings)) {
                is Result.Success -> {
                    _uiState.update { it.copy(userMessage = "Night mode settings saved") }
                    _events.emit("NightModeSettingsSaved")
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun setBubbleRadius(radius: Int) {
        scope.launch {
            when (val res = setBubbleRadiusUseCase(radius)) {
                is Result.Success -> {
                    _uiState.update { it.copy(bubbleRadius = radius) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun resetToDefaults() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = resetAppearanceSettingsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, userMessage = "Reset to defaults") }
                    _events.emit("ResetToDefaults")
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.error.message) }
                }
            }
        }
    }

    private fun clearMessage() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
