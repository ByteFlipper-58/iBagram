package org.telegram.messenger.feature.businessrecipients.domain.usecase

import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class GetBusinessRecipientsUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(): BusinessRecipientsModel {
        return repository.getRecipients()
    }
}
