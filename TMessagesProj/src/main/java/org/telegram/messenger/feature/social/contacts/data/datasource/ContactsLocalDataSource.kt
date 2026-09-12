package org.telegram.messenger.feature.social.contacts.data.datasource

import org.telegram.messenger.ContactsController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.core.data.BaseLocalDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

/**
 * Local data source for Telegram contacts operations accessing MessagesStorage (SQLite database)
 * on Dispatchers.IO and in-memory caches on the main thread safely.
 */
open class ContactsLocalDataSource(
    currentAccount: Int
) : BaseLocalDataSource(currentAccount) {

    private val contactsController: ContactsController?
        get() = try {
            ContactsController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private val messagesController: MessagesController?
        get() = try {
            MessagesController.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    /**
     * Retrieves currently loaded raw contacts from the in-memory cache.
     */
    open fun getRawContacts(): List<TLRPC.TL_contact> {
        val list = contactsController?.contacts ?: return emptyList()
        return ArrayList(list)
    }

    /**
     * Retrieves a single contact from the in-memory dictionary cache by user ID.
     */
    open fun getRawContact(userId: Long): TLRPC.TL_contact? {
        return contactsController?.contactsDict?.get(userId)
    }

    /**
     * Retrieves a cached user model from MessagesController.
     */
    open fun getUser(userId: Long): TLRPC.User? {
        return messagesController?.getUser(userId)
    }

    /**
     * Resolves an InputUser for a given User instance.
     */
    open fun getInputUser(user: TLRPC.User): TLRPC.InputUser? {
        return messagesController?.getInputUser(user)
    }

    /**
     * Checks if contacts have already been loaded into memory.
     */
    open fun isContactsLoaded(): Boolean {
        return contactsController?.contactsLoaded ?: false
    }

    /**
     * Triggers contacts loading in legacy controller if needed.
     */
    open fun triggerLegacyLoadContacts(fromCache: Boolean = false, hash: Long = 0L) {
        contactsController?.loadContacts(fromCache, hash)
    }

    /**
     * Persists contacts list into MessagesStorage SQLite database.
     */
    open suspend fun saveContactsToDb(
        contacts: ArrayList<TLRPC.TL_contact>,
        deleteAll: Boolean
    ): Result<Unit> = runOnDb { storage ->
        storage.putContacts(contacts, deleteAll)
    }

    /**
     * Deletes contact IDs from MessagesStorage SQLite database.
     */
    open suspend fun deleteContactsFromDb(uids: ArrayList<Long>): Result<Unit> = runOnDb { storage ->
        storage.deleteContacts(uids)
    }

    /**
     * Persists users to MessagesStorage SQLite database.
     */
    open suspend fun saveUsersToDb(
        users: ArrayList<TLRPC.User>,
        fromCache: Boolean = true,
        notify: Boolean = true
    ): Result<Unit> = runOnDb { storage ->
        storage.putUsersAndChats(users, null, fromCache, notify)
    }
}
