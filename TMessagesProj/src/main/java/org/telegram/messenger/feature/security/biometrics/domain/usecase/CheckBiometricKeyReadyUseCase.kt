package org.telegram.messenger.feature.security.biometrics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository

class CheckBiometricKeyReadyUseCase(
    private val repository: BiometricsRepository
) {
    suspend operator fun invoke(notifyCheckFingerprint: Boolean = true): Result<Boolean> {
        return repository.checkKeyReady(notifyCheckFingerprint)
    }
}
