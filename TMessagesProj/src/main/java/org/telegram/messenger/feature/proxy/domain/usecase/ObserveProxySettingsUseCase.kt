package org.telegram.messenger.feature.proxy.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.proxy.domain.model.ProxySettingsModel
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository

class ObserveProxySettingsUseCase(
    private val repository: ProxyRepository
) {
    operator fun invoke(): Flow<ProxySettingsModel> {
        return repository.observeProxySettings()
    }
}
