package org.telegram.messenger.feature.network.proxy.domain.usecase

import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

class GetProxySettingsUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(): ProxySettingsModel {
        return repository.getProxySettings()
    }
}
