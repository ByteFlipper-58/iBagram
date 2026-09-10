package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class CheckRecipientsChangesUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean {
        return repository.hasChanges(initial, current)
    }
}
