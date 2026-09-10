package org.telegram.messenger.feature.social.contacts.domain.usecase

import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository

class GetContactUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: Long): ContactModel? {
        return repository.getContact(userId)
    }
}
