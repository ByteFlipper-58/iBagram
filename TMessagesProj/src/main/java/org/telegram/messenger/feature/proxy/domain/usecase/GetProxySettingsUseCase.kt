package org.telegram.messenger.feature.proxy.domain.usecase

import org.telegram.messenger.feature.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository

class GetProxySettingsUseCase(
    private val repository: ProxyRepository
) {
    suspend operator fun invoke(): ProxySettingsModel {
        return repository.getProxySettings()
    }
}
