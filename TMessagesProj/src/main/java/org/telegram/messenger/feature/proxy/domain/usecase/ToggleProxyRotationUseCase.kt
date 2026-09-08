package org.telegram.messenger.feature.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository

class ToggleProxyRotationUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(enabled: Boolean, timeoutMinutes: Int = 10): Result<Unit> {
        return repository.toggleProxyRotation(enabled, timeoutMinutes)
    }
}
