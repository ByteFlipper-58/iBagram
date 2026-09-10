package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class ResetBusinessRecipientsUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke() {
        repository.reset()
    }
}
