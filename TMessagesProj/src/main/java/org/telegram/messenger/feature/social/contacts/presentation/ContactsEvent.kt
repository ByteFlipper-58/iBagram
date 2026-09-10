package org.telegram.messenger.feature.social.contacts.presentation

import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel

/**
 * Single-shot UI events emitted by [ContactsViewModel].
 */
sealed interface ContactsEvent {
    data class ContactAdded(val userId: Long) : ContactsEvent
    data class ContactDeleted(val userId: Long) : ContactsEvent
    data class ShowToast(val message: String) : ContactsEvent
    data class ShowError(val message: String) : ContactsEvent
}
