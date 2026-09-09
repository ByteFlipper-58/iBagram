package org.telegram.messenger.feature.biometrics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.biometrics.domain.repository.BiometricsRepository

class DeleteInvalidBiometricKeyUseCase(
    private val repository: BiometricsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.deleteInvalidKey()
    }
}
