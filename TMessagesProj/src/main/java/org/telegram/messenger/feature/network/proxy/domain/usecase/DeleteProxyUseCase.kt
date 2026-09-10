package org.telegram.messenger.feature.network.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.network.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

class DeleteProxyUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(proxy: ProxyModel): Result<Unit> {
        return repository.deleteProxy(proxy)
    }
}
