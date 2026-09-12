package org.telegram.messenger.feature.social.contacts.data.datasource

import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.core.result.Result
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.Vector
import java.util.ArrayList

/**
 * Remote data source executing MTProto RPC requests for Telegram contacts operations.
 */
open class ContactsRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    /**
     * Fetches contacts from MTProto server using current sync hash.
     */
    open suspend fun getContacts(hash: Long = 0L): Result<TLRPC.contacts_Contacts> {
        val req = TLRPC.TL_contacts_getContacts().apply {
            this.hash = hash
        }
        return executeRequest(req)
    }

    /**
     * Adds or edits a contact on MTProto server.
     */
    open suspend fun addContact(
        user: TLRPC.InputUser,
        firstName: String,
        lastName: String,
        phone: String,
        addPhonePrivacyException: Boolean = false,
        note: TLRPC.TL_textWithEntities? = null
    ): Result<TLRPC.Updates> {
        val req = TLRPC.TL_contacts_addContact().apply {
            this.id = user
            this.first_name = firstName
            this.last_name = lastName
            var cleanPhone = phone
            if (cleanPhone.isNotEmpty() && !cleanPhone.startsWith("+")) {
                cleanPhone = "+$cleanPhone"
            }
            this.phone = cleanPhone
            this.add_phone_privacy_exception = addPhonePrivacyException
            if (note != null) {
                this.flags = this.flags or 2
                this.note = note
            }
        }
        return executeRequest(req)
    }

    /**
     * Deletes a list of contacts from MTProto server.
     */
    open suspend fun deleteContacts(inputUsers: List<TLRPC.InputUser>): Result<TLRPC.Updates> {
        val req = TLRPC.TL_contacts_deleteContacts().apply {
            this.id = ArrayList(inputUsers)
        }
        return executeRequest(req)
    }

    /**
     * Searches for contacts and public users globally via MTProto server.
     */
    open suspend fun searchContacts(query: String, limit: Int = 50): Result<TLRPC.TL_contacts_found> {
        val req = TLRPC.TL_contacts_search().apply {
            this.q = query
            this.limit = limit
        }
        return executeRequest(req)
    }

    /**
     * Resets server-saved contacts for the account.
     */
    open suspend fun resetSavedContacts(): Result<TLRPC.Bool> {
        val req = TLRPC.TL_contacts_resetSaved()
        return executeRequest(req)
    }

    /**
     * Fetches current online statuses for contacts from MTProto server.
     */
    open suspend fun getStatuses(): Result<Vector<TLRPC.TL_contactStatus>> {
        val req = TLRPC.TL_contacts_getStatuses()
        return executeRequest(req)
    }
}
