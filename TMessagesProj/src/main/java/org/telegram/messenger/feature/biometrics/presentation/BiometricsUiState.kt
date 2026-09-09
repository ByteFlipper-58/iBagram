package org.telegram.messenger.feature.biometrics.presentation

import org.telegram.messenger.feature.biometrics.domain.model.BiometricKeyStateModel

sealed class BiometricsUiState {
    object Idle : BiometricsUiState()
    object Checking : BiometricsUiState()
    data class Ready(
        val keyState: BiometricKeyStateModel,
        val isDeleting: Boolean = false,
        val errorMessage: String? = null
    ) : BiometricsUiState()
    data class Error(val message: String?) : BiometricsUiState()
}
