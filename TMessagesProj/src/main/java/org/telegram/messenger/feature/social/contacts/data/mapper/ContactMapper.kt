package org.telegram.messenger.feature.social.contacts.data.mapper

import org.telegram.messenger.ContactsController
import org.telegram.messenger.UserObject
import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.tgnet.TLRPC

/**
 * Maps legacy Telegram contacts and user objects to clean [ContactModel] domain entities.
 */
object ContactMapper {

    fun toDomain(contact: TLRPC.TL_contact?, user: TLRPC.User?): ContactModel {
        val id = user?.id ?: contact?.user_id ?: 0L
        val firstName = user?.first_name ?: ""
        val lastName = user?.last_name ?: ""
        val displayName = if (user != null) {
            val formatted = ContactsController.formatName(firstName, lastName)
            if (!formatted.isNullOrBlank()) formatted else (UserObject.getUserName(user) ?: "")
        } else ""
        val username = user?.username ?: ""
        val phone = user?.phone ?: ""
        val isMutual = contact?.mutual ?: false
        val isOnline = user?.status is TLRPC.TL_userStatusOnline

        return ContactModel(
            id = id,
            firstName = firstName,
            lastName = lastName,
            displayName = displayName,
            username = username,
            phone = phone,
            isMutual = isMutual,
            isOnline = isOnline
        )
    }

    fun toDomainList(
        contacts: List<TLRPC.TL_contact>,
        userProvider: (Long) -> TLRPC.User?
    ): List<ContactModel> {
        return contacts.map { contact ->
            val user = userProvider(contact.user_id)
            toDomain(contact, user)
        }
    }
}
