package org.telegram.messenger.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.settings.domain.model.SettingsModel
import org.telegram.messenger.feature.settings.domain.usecase.GetSettingsUseCase
import org.telegram.messenger.feature.settings.domain.usecase.ObserveSettingsUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateBubbleRadiusUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateFontSizeUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateSaveToGalleryUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateStreamMediaUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateSyncContactsUseCase

/**
 * ViewModel managing application settings state and user actions.
 */
class SettingsViewModel(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateFontSizeUseCase: UpdateFontSizeUseCase,
    private val updateBubbleRadiusUseCase: UpdateBubbleRadiusUseCase,
    private val updateSaveToGalleryUseCase: UpdateSaveToGalleryUseCase,
    private val updateStreamMediaUseCase: UpdateStreamMediaUseCase,
    private val updateSyncContactsUseCase: UpdateSyncContactsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            observeSettingsUseCase().collect { settings ->
                _uiState.value = SettingsUiState.Success(
                    settings = settings,
                    isUpdating = false
                )
            }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch {
            try {
                val settings = getSettingsUseCase()
                _uiState.value = SettingsUiState.Success(
                    settings = settings,
                    isUpdating = false
                )
            } catch (e: Throwable) {
                _uiState.value = SettingsUiState.Error(e.message)
            }
        }
    }

    fun setFontSize(fontSize: Int) {
        updateSetting("fontSize") {
            updateFontSizeUseCase(fontSize)
        }
    }

    fun setBubbleRadius(radius: Int) {
        updateSetting("bubbleRadius") {
            updateBubbleRadiusUseCase(radius)
        }
    }

    fun setSaveToGallery(enabled: Boolean) {
        updateSetting("saveToGallery") {
            updateSaveToGalleryUseCase(enabled)
        }
    }

    fun setStreamMedia(enabled: Boolean) {
        updateSetting("streamMedia") {
            updateStreamMediaUseCase(enabled)
        }
    }

    fun setSyncContacts(enabled: Boolean) {
        updateSetting("syncContacts") {
            updateSyncContactsUseCase(enabled)
        }
    }

    private fun updateSetting(settingName: String, action: suspend () -> Result<Unit>) {
        val current = _uiState.value
        if (current is SettingsUiState.Success) {
            _uiState.value = current.copy(isUpdating = true)
        }
        viewModelScope.launch {
            when (val result = action()) {
                is Result.Success -> {
                    _events.send(SettingsEvent.SettingChanged(settingName))
                }
                is Result.Failure -> {
                    if (current is SettingsUiState.Success) {
                        _uiState.value = current.copy(isUpdating = false)
                    }
                    _events.send(SettingsEvent.ShowError(result.error.message))
                }
            }
        }
    }
}
