package org.telegram.messenger.feature.contacts.domain.usecase

import org.telegram.messenger.feature.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.contacts.domain.repository.ContactsRepository

class GetContactsUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(forceReload: Boolean = false): List<ContactModel> {
        return repository.getContacts(forceReload)
    }
}
