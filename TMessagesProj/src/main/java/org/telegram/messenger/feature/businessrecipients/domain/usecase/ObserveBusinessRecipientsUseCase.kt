package org.telegram.messenger.feature.businessrecipients.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class ObserveBusinessRecipientsUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(): StateFlow<BusinessRecipientsModel> {
        return repository.observeRecipients()
    }
}
