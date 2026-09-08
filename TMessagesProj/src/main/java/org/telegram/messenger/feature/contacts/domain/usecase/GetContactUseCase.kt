package org.telegram.messenger.feature.contacts.domain.usecase

import org.telegram.messenger.feature.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.contacts.domain.repository.ContactsRepository

class GetContactUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: Long): ContactModel? {
        return repository.getContact(userId)
    }
}
