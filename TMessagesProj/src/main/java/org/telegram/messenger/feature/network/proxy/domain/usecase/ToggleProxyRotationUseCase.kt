package org.telegram.messenger.feature.network.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

class ToggleProxyRotationUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(enabled: Boolean, timeoutMinutes: Int = 10): Result<Unit> {
        return repository.toggleProxyRotation(enabled, timeoutMinutes)
    }
}
