package org.telegram.messenger.feature.businessrecipients.domain.usecase

import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

class RemoveSelectedUserUseCase(
    private val repository: BusinessRecipientsRepository
) {
    operator fun invoke(userId: Long) {
        repository.removeSelectedUser(userId)
    }
}
