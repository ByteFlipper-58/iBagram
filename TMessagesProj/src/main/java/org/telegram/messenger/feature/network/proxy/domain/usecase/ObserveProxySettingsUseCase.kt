package org.telegram.messenger.feature.network.proxy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.network.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository

class ObserveProxySettingsUseCase(
    private val repository: ProxyRepository
) {
    operator fun invoke(): Flow<ProxySettingsModel> {
        return repository.observeProxySettings()
    }
}
