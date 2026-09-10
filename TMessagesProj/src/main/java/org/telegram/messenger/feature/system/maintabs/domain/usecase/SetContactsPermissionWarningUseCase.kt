package org.telegram.messenger.feature.system.maintabs.domain.usecase

import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class SetContactsPermissionWarningUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(hasWarning: Boolean) {
        repository.setContactsPermissionWarning(hasWarning)
    }
}
