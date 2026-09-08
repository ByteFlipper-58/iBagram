package org.telegram.messenger.feature.datastorage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.datastorage.domain.model.AutoDownloadNetworkType
import org.telegram.messenger.feature.datastorage.domain.model.NetworkUsageType
import org.telegram.messenger.feature.datastorage.domain.usecase.ClearCacheUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ClearDatabaseUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetKeepMediaSettingsUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveKeepMediaSettingsUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.RefreshStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ResetNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.UpdateAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.UpdateKeepMediaUseCase

class DataStorageViewModel(
    private val observeNetworkUsageUseCase: ObserveNetworkUsageUseCase,
    private val observeStorageUsageUseCase: ObserveStorageUsageUseCase,
    private val observeAutoDownloadPresetUseCase: ObserveAutoDownloadPresetUseCase,
    private val observeKeepMediaSettingsUseCase: ObserveKeepMediaSettingsUseCase,
    private val getNetworkUsageUseCase: GetNetworkUsageUseCase,
    private val resetNetworkUsageUseCase: ResetNetworkUsageUseCase,
    private val getStorageUsageUseCase: GetStorageUsageUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val clearDatabaseUseCase: ClearDatabaseUseCase,
    private val getAutoDownloadPresetUseCase: GetAutoDownloadPresetUseCase,
    private val updateAutoDownloadPresetUseCase: UpdateAutoDownloadPresetUseCase,
    private val getKeepMediaSettingsUseCase: GetKeepMediaSettingsUseCase,
    private val updateKeepMediaUseCase: UpdateKeepMediaUseCase,
    private val refreshStorageUsageUseCase: RefreshStorageUsageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataStorageUiState(isLoading = true))
    val uiState: StateFlow<DataStorageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DataStorageEvent>()
    val events: SharedFlow<DataStorageEvent> = _events.asSharedFlow()

    init {
        observeStorage()
        observePresets()
        observeKeepMedia()
        loadInitialData()
    }

    private fun observeStorage() {
        viewModelScope.launch {
            observeStorageUsageUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { usage ->
                    _uiState.update { it.copy(storageUsage = usage, isLoading = false) }
                }
        }
    }

    private fun observePresets() {
        viewModelScope.launch {
            observeAutoDownloadPresetUseCase(AutoDownloadNetworkType.MOBILE)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { preset -> _uiState.update { it.copy(mobilePreset = preset) } }
        }
        viewModelScope.launch {
            observeAutoDownloadPresetUseCase(AutoDownloadNetworkType.WIFI)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { preset -> _uiState.update { it.copy(wifiPreset = preset) } }
        }
        viewModelScope.launch {
            observeAutoDownloadPresetUseCase(AutoDownloadNetworkType.ROAMING)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { preset -> _uiState.update { it.copy(roamingPreset = preset) } }
        }
    }

    private fun observeKeepMedia() {
        viewModelScope.launch {
            observeKeepMediaSettingsUseCase()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { settings -> _uiState.update { it.copy(keepMediaSettings = settings) } }
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getStorageUsageUseCase()
            getNetworkUsageUseCase(NetworkUsageType.MOBILE)
            getNetworkUsageUseCase(NetworkUsageType.WIFI)
            getNetworkUsageUseCase(NetworkUsageType.ROAMING)
            getAutoDownloadPresetUseCase(AutoDownloadNetworkType.MOBILE)
            getAutoDownloadPresetUseCase(AutoDownloadNetworkType.WIFI)
            getAutoDownloadPresetUseCase(AutoDownloadNetworkType.ROAMING)
            getKeepMediaSettingsUseCase()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onEvent(event: DataStorageEvent) {
        viewModelScope.launch {
            _events.emit(event)
            when (event) {
                is DataStorageEvent.RefreshStorage -> {
                    refreshStorageUsageUseCase()
                }
                is DataStorageEvent.ResetNetwork -> {
                    resetNetworkUsageUseCase(event.type)
                }
                is DataStorageEvent.ClearCache -> {
                    clearCacheUseCase(
                        clearPhotos = event.clearPhotos,
                        clearVideos = event.clearVideos,
                        clearDocuments = event.clearDocuments,
                        clearMusic = event.clearMusic,
                        clearAudio = event.clearAudio,
                        clearStickers = event.clearStickers,
                        clearStories = event.clearStories,
                        clearOther = event.clearOther
                    )
                }
                is DataStorageEvent.ClearDatabase -> {
                    clearDatabaseUseCase()
                }
                is DataStorageEvent.UpdatePreset -> {
                    updateAutoDownloadPresetUseCase(event.preset)
                }
                is DataStorageEvent.UpdateKeepMedia -> {
                    updateKeepMediaUseCase(event.chatType, event.duration)
                }
            }
        }
    }
}
