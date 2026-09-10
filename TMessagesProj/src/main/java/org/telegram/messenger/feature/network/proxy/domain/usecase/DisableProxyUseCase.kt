package org.telegram.messenger.feature.network.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

class DisableProxyUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.disableProxy()
    }
}
