package org.telegram.messenger.feature.businessrecipients.domain.usecase

import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientValidationResult
import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class ValidateBusinessRecipientsUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(model: BusinessRecipientsModel): RecipientValidationResult {
        return repository.validate(model)
    }
}
