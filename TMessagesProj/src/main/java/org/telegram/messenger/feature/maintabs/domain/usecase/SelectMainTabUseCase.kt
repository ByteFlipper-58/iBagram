package org.telegram.messenger.feature.maintabs.domain.usecase

import org.telegram.messenger.feature.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class SelectMainTabUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(tab: MainTabType) {
        repository.selectTab(tab)
    }

    operator fun invoke(position: Int) {
        repository.selectPosition(position)
    }
}
