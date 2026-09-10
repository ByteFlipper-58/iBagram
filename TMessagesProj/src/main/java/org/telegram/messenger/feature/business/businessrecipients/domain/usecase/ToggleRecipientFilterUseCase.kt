package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class ToggleRecipientFilterUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(filter: RecipientFilterType, enabled: Boolean) {
        repository.toggleFilter(filter, enabled)
    }
}
