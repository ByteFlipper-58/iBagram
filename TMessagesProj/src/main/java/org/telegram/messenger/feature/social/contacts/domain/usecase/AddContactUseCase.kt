package org.telegram.messenger.feature.social.contacts.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository

class AddContactUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(
        userId: Long,
        firstName: String,
        lastName: String,
        phone: String
    ): Result<Unit> {
        return repository.addContact(userId, firstName, lastName, phone)
    }
}
