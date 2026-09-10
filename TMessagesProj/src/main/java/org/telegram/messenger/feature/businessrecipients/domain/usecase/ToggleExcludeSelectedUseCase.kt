package org.telegram.messenger.feature.businessrecipients.domain.usecase

import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class ToggleExcludeSelectedUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(exclude: Boolean) {
        repository.toggleExcludeSelected(exclude)
    }
}
