package org.telegram.messenger.feature.business.businessrecipients.domain.usecase

import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class AddSelectedUsersUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(userIds: Collection<Long>) {
        repository.addSelectedUsers(userIds)
    }
}
