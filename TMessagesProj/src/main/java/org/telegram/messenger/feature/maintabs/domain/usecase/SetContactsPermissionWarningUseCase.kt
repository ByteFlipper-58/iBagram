package org.telegram.messenger.feature.maintabs.domain.usecase

import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository

class SetContactsPermissionWarningUseCase(
    private val repository: MainTabsRepository
) {
    operator fun invoke(hasWarning: Boolean) {
        repository.setContactsPermissionWarning(hasWarning)
    }
}
