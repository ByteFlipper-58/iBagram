package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class ToggleExcludeSelectedUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(exclude: Boolean) {
        repository.toggleExcludeSelected(exclude)
    }
}
