package org.telegram.messenger.feature.proxy.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository

class AddProxyUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(
        address: String,
        port: Int,
        username: String = "",
        password: String = "",
        secret: String = ""
    ): Result<ProxyModel> {
        return repository.addProxy(
            address = address,
            port = port,
            username = username,
            password = password,
            secret = secret
        )
    }
}
