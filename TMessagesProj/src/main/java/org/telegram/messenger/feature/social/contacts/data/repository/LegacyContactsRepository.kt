package org.telegram.messenger.feature.social.contacts.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.data.mapper.ContactMapper
import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository
import org.telegram.tgnet.TLRPC

/**
 * Clean adapter implementing [ContactsRepository] backed by legacy [ContactsController] and [MessagesController].
 */
class LegacyContactsRepository(
    private val currentAccount: Int
) : ContactsRepository {

    private val contactsController: ContactsController
        get() = ContactsController.getInstance(currentAccount)

    private val messagesController: MessagesController
        get() = MessagesController.getInstance(currentAccount)

    private fun readContacts(): List<ContactModel> {
        val rawContacts = ArrayList(contactsController.contacts)
        return ContactMapper.toDomainList(rawContacts) { userId ->
            messagesController.getUser(userId)
        }
    }

    override fun observeContacts(): Flow<List<ContactModel>> = callbackFlow {
        fun emitCurrent() {
            trySend(readContacts())
        }

        val delegate = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            when (id) {
                NotificationCenter.contactsDidLoad,
                NotificationCenter.updateInterfaces -> emitCurrent()
            }
        }

        val nc = NotificationCenter.getInstance(currentAccount)
        nc.addObserver(delegate, NotificationCenter.contactsDidLoad)
        nc.addObserver(delegate, NotificationCenter.updateInterfaces)

        if (!contactsController.contactsLoaded) {
            contactsController.loadContacts(false, 0)
        }
        emitCurrent()

        awaitClose {
            nc.removeObserver(delegate, NotificationCenter.contactsDidLoad)
            nc.removeObserver(delegate, NotificationCenter.updateInterfaces)
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getContacts(forceReload: Boolean): List<ContactModel> = withContext(Dispatchers.Main) {
        if (forceReload || !contactsController.contactsLoaded) {
            contactsController.loadContacts(false, 0)
        }
        readContacts()
    }

    override suspend fun getContact(userId: Long): ContactModel? = withContext(Dispatchers.Main) {
        val contact = contactsController.contactsDict[userId]
        val user = messagesController.getUser(userId)
        if (contact == null && user == null) {
            null
        } else {
            ContactMapper.toDomain(contact, user)
        }
    }

    override suspend fun addContact(
        userId: Long,
        firstName: String,
        lastName: String,
        phone: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            var user = messagesController.getUser(userId)
            if (user == null) {
                user = TLRPC.TL_user().apply {
                    id = userId
                    first_name = firstName
                    last_name = lastName
                    this.phone = phone
                }
            } else {
                user.first_name = firstName
                user.last_name = lastName
                user.phone = phone
            }
            contactsController.addContact(user, false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to add contact for user $userId", e))
        }
    }

    override suspend fun deleteContact(userId: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val user = messagesController.getUser(userId) ?: TLRPC.TL_user().apply { id = userId }
            contactsController.deleteContact(arrayListOf(user), false)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to delete contact for user $userId", e))
        }
    }

    override suspend fun searchContacts(query: String): List<ContactModel> = withContext(Dispatchers.Main) {
        val all = readContacts()
        if (query.isBlank()) {
            all
        } else {
            val cleanQuery = query.trim().lowercase()
            all.filter { contact ->
                contact.displayName.lowercase().contains(cleanQuery) ||
                contact.username.lowercase().contains(cleanQuery) ||
                contact.phone.lowercase().contains(cleanQuery)
            }
        }
    }
}
