package org.telegram.messenger.feature.contacts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.contacts.domain.repository.ContactsRepository

class DeleteContactUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: Long): Result<Unit> {
        return repository.deleteContact(userId)
    }
}
