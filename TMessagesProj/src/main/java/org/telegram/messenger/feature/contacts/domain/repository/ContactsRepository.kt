package org.telegram.messenger.feature.contacts.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.contacts.domain.model.ContactModel

/**
 * Clean domain repository contract for managing contacts.
 */
interface ContactsRepository {
    fun observeContacts(): Flow<List<ContactModel>>
    suspend fun getContacts(forceReload: Boolean = false): List<ContactModel>
    suspend fun getContact(userId: Long): ContactModel?
    suspend fun addContact(userId: Long, firstName: String, lastName: String, phone: String): Result<Unit>
    suspend fun deleteContact(userId: Long): Result<Unit>
    suspend fun searchContacts(query: String): List<ContactModel>
}
