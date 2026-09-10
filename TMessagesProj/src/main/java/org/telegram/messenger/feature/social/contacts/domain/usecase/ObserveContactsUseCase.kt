package org.telegram.messenger.feature.social.contacts.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository

class ObserveContactsUseCase(
    private val repository: ContactsRepository
) {
    operator fun invoke(): Flow<List<ContactModel>> {
        return repository.observeContacts()
    }
}
