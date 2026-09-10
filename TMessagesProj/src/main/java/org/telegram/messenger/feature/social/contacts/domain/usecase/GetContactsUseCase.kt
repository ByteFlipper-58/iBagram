package org.telegram.messenger.feature.social.contacts.domain.usecase

import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository

class GetContactsUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(forceReload: Boolean = false): List<ContactModel> {
        return repository.getContacts(forceReload)
    }
}
