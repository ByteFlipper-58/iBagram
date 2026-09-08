package org.telegram.messenger.feature.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository

class CheckProxyPingUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(proxy: ProxyModel): Result<Long> {
        return repository.checkProxyPing(proxy)
    }
}
