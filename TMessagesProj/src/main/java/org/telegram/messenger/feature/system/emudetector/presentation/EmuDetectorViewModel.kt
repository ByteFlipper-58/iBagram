package org.telegram.messenger.feature.system.emudetector.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.system.emudetector.domain.usecase.AddCustomPackageNameUseCase
import org.telegram.messenger.feature.system.emudetector.domain.usecase.ClearDetectorCacheUseCase
import org.telegram.messenger.feature.system.emudetector.domain.usecase.DetectEnvironmentUseCase
import org.telegram.messenger.feature.system.emudetector.domain.usecase.GetDetectorConfigUseCase
import org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveDiagnosticsUseCase
import org.telegram.messenger.feature.system.emudetector.domain.usecase.UpdateDetectorConfigUseCase

class EmuDetectorViewModel(
    private val detectEnvironmentUseCase: DetectEnvironmentUseCase,
    private val observeDiagnosticsUseCase: ObserveDiagnosticsUseCase,
    private val getDetectorConfigUseCase: GetDetectorConfigUseCase,
    private val updateDetectorConfigUseCase: UpdateDetectorConfigUseCase,
    private val addCustomPackageNameUseCase: AddCustomPackageNameUseCase,
    private val clearDetectorCacheUseCase: ClearDetectorCacheUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _uiState = MutableStateFlow(
        EmuDetectorUiState(
            config = getDetectorConfigUseCase()
        )
    )
    val uiState: StateFlow<EmuDetectorUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeDiagnosticsUseCase().collect { diagnostics ->
                _uiState.update { it.copy(diagnostics = diagnostics) }
            }
        }
    }

    fun onEvent(event: EmuDetectorEvent) {
        when (event) {
            is EmuDetectorEvent.RunDetection -> runDetection(event.forceRefresh)
            is EmuDetectorEvent.UpdateConfig -> updateConfig(event.config)
            is EmuDetectorEvent.AddCustomPackage -> addCustomPackage(event.packageName)
            is EmuDetectorEvent.ClearCache -> clearCache()
            is EmuDetectorEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun runDetection(forceRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            try {
                val diagnostics = detectEnvironmentUseCase(forceRefresh)
                _uiState.update { it.copy(isLoading = false, diagnostics = diagnostics) }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to detect environment"
                    )
                }
            }
        }
    }

    private fun updateConfig(config: EmulatorDetectorConfig) {
        updateDetectorConfigUseCase(config)
        _uiState.update { it.copy(config = config) }
    }

    private fun addCustomPackage(packageName: String) {
        addCustomPackageNameUseCase(packageName)
        _uiState.update { it.copy(config = getDetectorConfigUseCase()) }
    }

    private fun clearCache() {
        clearDetectorCacheUseCase()
        _uiState.update { it.copy(diagnostics = null) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
