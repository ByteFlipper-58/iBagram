package org.telegram.messenger.feature.maintabs.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class ObserveMainTabsConfigUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(): Flow<MainTabsConfigModel> = repository.observeConfig()
}
