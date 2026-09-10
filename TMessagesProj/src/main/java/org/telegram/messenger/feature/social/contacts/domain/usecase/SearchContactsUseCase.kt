package org.telegram.messenger.feature.social.contacts.domain.usecase

import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository

class SearchContactsUseCase(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(query: String): List<ContactModel> {
        return repository.searchContacts(query)
    }
}
