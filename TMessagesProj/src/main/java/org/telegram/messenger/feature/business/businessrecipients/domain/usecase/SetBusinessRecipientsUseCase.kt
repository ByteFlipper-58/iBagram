package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class SetBusinessRecipientsUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(model: BusinessRecipientsModel) {
        repository.setRecipients(model)
    }
}
