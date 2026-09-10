package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class RemoveExcludedUserUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(userId: Long) {
        repository.removeExcludedUser(userId)
    }
}
