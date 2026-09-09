package org.telegram.messenger.feature.maintabs.domain.usecase

import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class SetShowCallsTabUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(show: Boolean) {
        repository.setShowCallsTab(show)
    }
}
