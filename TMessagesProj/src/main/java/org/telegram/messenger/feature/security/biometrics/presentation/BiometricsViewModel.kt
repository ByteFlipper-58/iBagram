package org.telegram.messenger.feature.security.biometrics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.security.biometrics.domain.usecase.CheckBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.DeleteInvalidBiometricKeyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.GetBiometricKeyStateUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.HasDeviceBiometricsChangedUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.IsBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.ObserveBiometricKeyStateUseCase

class BiometricsViewModel(
    private val observeBiometricKeyStateUseCase: ObserveBiometricKeyStateUseCase,
    private val getBiometricKeyStateUseCase: GetBiometricKeyStateUseCase,
    private val checkBiometricKeyReadyUseCase: CheckBiometricKeyReadyUseCase,
    private val deleteInvalidBiometricKeyUseCase: DeleteInvalidBiometricKeyUseCase,
    private val isBiometricKeyReadyUseCase: IsBiometricKeyReadyUseCase,
    private val hasDeviceBiometricsChangedUseCase: HasDeviceBiometricsChangedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BiometricsUiState>(BiometricsUiState.Idle)
    val uiState: StateFlow<BiometricsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeBiometricKeyStateUseCase().collect { keyState ->
                val current = _uiState.value
                if (current is BiometricsUiState.Ready) {
                    _uiState.value = current.copy(keyState = keyState)
                } else {
                    _uiState.value = BiometricsUiState.Ready(keyState = keyState)
                }
            }
        }
    }

    fun onEvent(event: BiometricsEvent) {
        when (event) {
            is BiometricsEvent.CheckKeyReady -> checkKeyReady(event.notifyCheckFingerprint)
            is BiometricsEvent.DeleteInvalidKey -> deleteInvalidKey()
            is BiometricsEvent.RefreshState -> refreshState()
            is BiometricsEvent.ClearError -> clearError()
        }
    }

    private fun checkKeyReady(notifyCheckFingerprint: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is BiometricsUiState.Ready) {
                _uiState.value = BiometricsUiState.Checking
            }
            when (val result = checkBiometricKeyReadyUseCase(notifyCheckFingerprint)) {
                is Result.Success -> {
                    val keyState = getBiometricKeyStateUseCase()
                    _uiState.value = BiometricsUiState.Ready(keyState = keyState)
                }
                is Result.Failure -> {
                    _uiState.value = BiometricsUiState.Error(result.error.message)
                }
            }
        }
    }

    private fun deleteInvalidKey() {
        val current = _uiState.value as? BiometricsUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isDeleting = true)
            when (val result = deleteInvalidBiometricKeyUseCase()) {
                is Result.Success -> {
                    val keyState = getBiometricKeyStateUseCase()
                    _uiState.value = current.copy(isDeleting = false, keyState = keyState)
                }
                is Result.Failure -> {
                    _uiState.value = current.copy(isDeleting = false, errorMessage = result.error.message)
                }
            }
        }
    }

    private fun refreshState() {
        viewModelScope.launch {
            val keyState = getBiometricKeyStateUseCase()
            val current = _uiState.value
            if (current is BiometricsUiState.Ready) {
                _uiState.value = current.copy(keyState = keyState)
            } else {
                _uiState.value = BiometricsUiState.Ready(keyState = keyState)
            }
        }
    }

    private fun clearError() {
        val current = _uiState.value as? BiometricsUiState.Ready ?: return
        _uiState.value = current.copy(errorMessage = null)
    }

    fun isKeyReady(): Boolean = isBiometricKeyReadyUseCase()

    fun hasDeviceBiometricsChanged(): Boolean = hasDeviceBiometricsChangedUseCase()
}
