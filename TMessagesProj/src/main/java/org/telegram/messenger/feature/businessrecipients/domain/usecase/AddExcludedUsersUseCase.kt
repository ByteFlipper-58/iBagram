package org.telegram.messenger.feature.businessrecipients.domain.usecase

import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class AddExcludedUsersUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(userIds: Collection<Long>) {
        repository.addExcludedUsers(userIds)
    }
}
