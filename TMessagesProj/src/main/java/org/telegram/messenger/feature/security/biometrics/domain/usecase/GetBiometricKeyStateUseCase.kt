package org.telegram.messenger.feature.security.biometrics.domain.usecase

import org.telegram.messenger.feature.security.biometrics.domain.model.BiometricKeyStateModel
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository

class GetBiometricKeyStateUseCase(
    private val repository: BiometricsRepository
) {
    suspend operator fun invoke(): BiometricKeyStateModel {
        return repository.getKeyState()
    }
}
