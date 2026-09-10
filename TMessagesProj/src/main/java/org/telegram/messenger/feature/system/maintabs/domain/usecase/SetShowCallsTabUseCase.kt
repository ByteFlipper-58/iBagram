package org.telegram.messenger.feature.system.maintabs.domain.usecase

import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class SetShowCallsTabUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(show: Boolean) {
        repository.setShowCallsTab(show)
    }
}
