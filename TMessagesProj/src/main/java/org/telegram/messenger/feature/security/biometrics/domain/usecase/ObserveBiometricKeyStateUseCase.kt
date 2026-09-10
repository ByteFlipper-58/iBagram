package org.telegram.messenger.feature.security.biometrics.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository

class ObserveBiometricKeyStateUseCase(
    private val repository: BiometricsRepository
) {
    operator fun invoke(): Flow<BiometricKeyStateModel> {
        return repository.observeKeyState()
    }
}
