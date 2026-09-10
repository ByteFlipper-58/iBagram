package org.telegram.messenger.feature.system.maintabs.domain.usecase

import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class GetMainTabsConfigUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(): MainTabsConfigModel = repository.getConfig()
}
