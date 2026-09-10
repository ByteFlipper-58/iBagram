package org.telegram.messenger.feature.system.maintabs.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class ObserveMainTabsConfigUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(): Flow<MainTabsConfigModel> = repository.observeConfig()
}
