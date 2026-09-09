package org.telegram.messenger.feature.maintabs.domain.usecase

import org.telegram.messenger.feature.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class GetMainTabsConfigUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(): MainTabsConfigModel = repository.getConfig()
}
