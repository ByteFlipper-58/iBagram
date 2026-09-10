package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class AddExcludedUsersUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(userIds: Collection<Long>) {
        repository.addExcludedUsers(userIds)
    }
}
