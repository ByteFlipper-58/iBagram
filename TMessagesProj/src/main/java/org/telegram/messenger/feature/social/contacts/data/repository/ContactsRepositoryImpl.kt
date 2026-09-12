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
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.data.datasource.ContactsLocalDataSource
import org.telegram.messenger.feature.social.contacts.data.datasource.ContactsRemoteDataSource
import org.telegram.messenger.feature.social.contacts.data.mapper.ContactMapper
import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

/**
 * Clean repository implementation coordinating local and remote data sources for contacts operations,
 * incrementally displacing monolithic legacy logic in ContactsController.
 */
class ContactsRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: ContactsLocalDataSource,
    private val remoteDataSource: ContactsRemoteDataSource
) : ContactsRepository {

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

    private val notificationCenter: NotificationCenter?
        get() = try {
            NotificationCenter.getInstance(currentAccount)
        } catch (e: Throwable) {
            null
        }

    private fun readContacts(): List<ContactModel> {
        val rawContacts = localDataSource.getRawContacts()
        return ContactMapper.toDomainList(rawContacts) { userId ->
            localDataSource.getUser(userId)
        }
    }

    override fun observeContacts(): Flow<List<ContactModel>> = callbackFlow {
        fun emitCurrent() {
            trySend(readContacts())
        }

        val observer = NotificationCenter.NotificationCenterDelegate { id, _, _ ->
            when (id) {
                NotificationCenter.contactsDidLoad,
                NotificationCenter.updateInterfaces -> emitCurrent()
            }
        }

        try {
            notificationCenter?.addObserver(observer, NotificationCenter.contactsDidLoad)
            notificationCenter?.addObserver(observer, NotificationCenter.updateInterfaces)
        } catch (e: Throwable) {
            // Safely ignored in headless test environment
        }

        if (!localDataSource.isContactsLoaded()) {
            localDataSource.triggerLegacyLoadContacts(false, 0L)
        }
        emitCurrent()

        awaitClose {
            try {
                notificationCenter?.removeObserver(observer, NotificationCenter.contactsDidLoad)
                notificationCenter?.removeObserver(observer, NotificationCenter.updateInterfaces)
            } catch (e: Throwable) {
                // Safely ignored in headless test environment
            }
        }
    }.flowOn(Dispatchers.Main)

    override suspend fun getContacts(forceReload: Boolean): List<ContactModel> = withContext(Dispatchers.Main) {
        if (forceReload || !localDataSource.isContactsLoaded()) {
            localDataSource.triggerLegacyLoadContacts(false, 0L)
        }
        readContacts()
    }

    override suspend fun getContact(userId: Long): ContactModel? = withContext(Dispatchers.Main) {
        val rawContact = localDataSource.getRawContact(userId)
        val user = localDataSource.getUser(userId)
        if (rawContact == null && user == null) {
            null
        } else {
            ContactMapper.toDomain(rawContact, user)
        }
    }

    override suspend fun addContact(
        userId: Long,
        firstName: String,
        lastName: String,
        phone: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            var user = localDataSource.getUser(userId)
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

            // Sync with remote server if InputUser is resolvable
            val inputUser = localDataSource.getInputUser(user)
            if (inputUser != null) {
                remoteDataSource.addContact(
                    user = inputUser,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    addPhonePrivacyException = false
                )
            }

            // Coordinate with legacy controller for in-memory dictionaries and system phonebook updates
            contactsController?.addContact(user, false)
            notifyContactsUpdated()

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to add contact for user $userId", e))
        }
    }

    override suspend fun deleteContact(userId: Long): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val user = localDataSource.getUser(userId) ?: TLRPC.TL_user().apply { id = userId }
            val inputUser = localDataSource.getInputUser(user)
            if (inputUser != null) {
                remoteDataSource.deleteContacts(listOf(inputUser))
            }

            // Update SQLite persistence
            localDataSource.deleteContactsFromDb(arrayListOf(userId))

            // Coordinate with legacy controller
            val usersList = ArrayList<TLRPC.User>().apply { add(user) }
            contactsController?.deleteContact(usersList, false)
            notifyContactsUpdated()

            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic("Failed to delete contact for user $userId", e))
        }
    }

    override suspend fun searchContacts(query: String): List<ContactModel> = withContext(Dispatchers.Main) {
        val allContacts = readContacts()
        if (query.isBlank()) {
            allContacts
        } else {
            val cleanQuery = query.trim().lowercase()
            allContacts.filter { contact ->
                contact.displayName.lowercase().contains(cleanQuery) ||
                contact.firstName.lowercase().contains(cleanQuery) ||
                contact.lastName.lowercase().contains(cleanQuery) ||
                contact.username.lowercase().contains(cleanQuery) ||
                contact.phone.lowercase().contains(cleanQuery)
            }
        }
    }

    private fun notifyContactsUpdated() {
        try {
            notificationCenter?.postNotificationName(NotificationCenter.contactsDidLoad)
        } catch (e: Throwable) {
            // Safely ignored in headless test environment
        }
    }
}
