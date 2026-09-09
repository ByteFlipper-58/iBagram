package org.telegram.messenger.feature.biometrics.domain.usecase

import org.telegram.messenger.feature.biometrics.domain.repository.BiometricsRepository

class IsBiometricKeyReadyUseCase(
    private val repository: BiometricsRepository
) {
    operator fun invoke(): Boolean {
        return repository.isKeyReady()
    }
}
