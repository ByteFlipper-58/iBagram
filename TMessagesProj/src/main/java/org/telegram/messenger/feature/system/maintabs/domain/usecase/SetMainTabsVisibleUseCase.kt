package org.telegram.messenger.feature.system.maintabs.domain.usecase

import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class SetMainTabsVisibleUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(visible: Boolean) {
        repository.setTabsVisible(visible)
    }
}
