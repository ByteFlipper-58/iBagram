package org.telegram.messenger.feature.social.contacts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.data.datasource.ContactsLocalDataSource
import org.telegram.messenger.feature.social.contacts.data.datasource.ContactsRemoteDataSource
import org.telegram.messenger.feature.social.contacts.data.repository.ContactsRepositoryImpl
import org.telegram.tgnet.TLRPC
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        AccountFeatureContainer.resetAll()
    }

    @After
    fun tearDown() {
        AccountFeatureContainer.resetAll()
        Dispatchers.resetMain()
    }

    // Fake local data source with controllable in-memory data
    private class FakeContactsLocalDataSource(account: Int) : ContactsLocalDataSource(account) {
        val contacts = mutableListOf<TLRPC.TL_contact>()
        val users = mutableMapOf<Long, TLRPC.User>()
        var loaded = false
        var loadTriggered = false

        override fun getRawContacts(): List<TLRPC.TL_contact> = ArrayList(contacts)

        override fun getRawContact(userId: Long): TLRPC.TL_contact? = contacts.firstOrNull { it.user_id == userId }

        override fun getUser(userId: Long): TLRPC.User? = users[userId]

        override fun getInputUser(user: TLRPC.User): TLRPC.InputUser {
            return TLRPC.TL_inputUser().apply {
                this.user_id = user.id
                this.access_hash = user.access_hash
            }
        }

        override fun isContactsLoaded(): Boolean = loaded

        override fun triggerLegacyLoadContacts(fromCache: Boolean, hash: Long) {
            loadTriggered = true
            loaded = true
        }

        override suspend fun saveContactsToDb(
            contacts: ArrayList<TLRPC.TL_contact>,
            deleteAll: Boolean
        ): Result<Unit> {
            if (deleteAll) {
                this.contacts.clear()
            }
            this.contacts.addAll(contacts)
            return Result.success(Unit)
        }

        override suspend fun deleteContactsFromDb(uids: ArrayList<Long>): Result<Unit> {
            this.contacts.removeAll { uids.contains(it.user_id) }
            return Result.success(Unit)
        }

        override suspend fun saveUsersToDb(
            users: ArrayList<TLRPC.User>,
            fromCache: Boolean,
            notify: Boolean
        ): Result<Unit> {
            for (u in users) {
                this.users[u.id] = u
            }
            return Result.success(Unit)
        }
    }

    // Fake remote data source recording calls
    private class FakeContactsRemoteDataSource(account: Int) : ContactsRemoteDataSource(account) {
        val addedContacts = mutableListOf<Triple<TLRPC.InputUser, String, String>>()
        val deletedUsers = mutableListOf<List<TLRPC.InputUser>>()

        override suspend fun addContact(
            user: TLRPC.InputUser,
            firstName: String,
            lastName: String,
            phone: String,
            addPhonePrivacyException: Boolean,
            note: TLRPC.TL_textWithEntities?
        ): Result<TLRPC.Updates> {
            addedContacts.add(Triple(user, firstName, phone))
            return Result.success(TLRPC.TL_updates())
        }

        override suspend fun deleteContacts(inputUsers: List<TLRPC.InputUser>): Result<TLRPC.Updates> {
            deletedUsers.add(inputUsers)
            return Result.success(TLRPC.TL_updates())
        }
    }

    @Test
    fun testGetContactsReturnsMappedList() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        local.contacts.add(TLRPC.TL_contact().apply { user_id = 100L; mutual = true })
        local.users[100L] = TLRPC.TL_user().apply {
            id = 100L
            first_name = "Alice"
            last_name = "Wonder"
            phone = "+123456789"
        }

        val list = repo.getContacts()
        assertTrue(local.loadTriggered)
        assertEquals(1, list.size)
        assertEquals(100L, list[0].id)
        assertEquals("Alice", list[0].firstName)
        assertEquals("+123456789", list[0].phone)
        assertTrue(list[0].isMutual)
    }

    @Test
    fun testGetContactReturnsMappedContact() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        local.contacts.add(TLRPC.TL_contact().apply { user_id = 200L })
        local.users[200L] = TLRPC.TL_user().apply {
            id = 200L
            first_name = "Bob"
            last_name = "Marley"
            username = "bobmarley"
        }

        val contact = repo.getContact(200L)
        assertNotNull(contact)
        assertEquals("Bob", contact?.firstName)
        assertEquals("Marley", contact?.lastName)
        assertEquals("bobmarley", contact?.username)

        val missing = repo.getContact(999L)
        assertNull(missing)
    }

    @Test
    fun testAddContactCoordinatesRemoteAndLocal() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        val result = repo.addContact(
            userId = 300L,
            firstName = "Charlie",
            lastName = "Brown",
            phone = "+987654321"
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun testDeleteContactCoordinatesRemoteAndLocal() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        local.contacts.add(TLRPC.TL_contact().apply { user_id = 400L })
        local.users[400L] = TLRPC.TL_user().apply { id = 400L }

        val result = repo.deleteContact(400L)
        assertTrue(result.isSuccess)
        assertFalse(local.contacts.any { it.user_id == 400L })
    }

    @Test
    fun testSearchContactsFiltersCorrectly() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        local.contacts.add(TLRPC.TL_contact().apply { user_id = 1L })
        local.contacts.add(TLRPC.TL_contact().apply { user_id = 2L })
        local.users[1L] = TLRPC.TL_user().apply {
            id = 1L
            first_name = "John"
            last_name = "Doe"
            username = "johndoe"
            phone = "+111222"
        }
        local.users[2L] = TLRPC.TL_user().apply {
            id = 2L
            first_name = "Jane"
            last_name = "Smith"
            username = "janesmith"
            phone = "+333444"
        }

        val all = repo.searchContacts("")
        assertEquals(2, all.size)

        val john = repo.searchContacts("John")
        assertEquals(1, john.size)
        assertEquals(1L, john[0].id)

        val smith = repo.searchContacts("smith")
        assertEquals(1, smith.size)
        assertEquals(2L, smith[0].id)

        val none = repo.searchContacts("nobody")
        assertEquals(0, none.size)
    }

    @Test
    fun testObserveContactsEmitsList() = runTest {
        val local = FakeContactsLocalDataSource(0)
        val remote = FakeContactsRemoteDataSource(0)
        val repo = ContactsRepositoryImpl(0, local, remote)

        local.contacts.add(TLRPC.TL_contact().apply { user_id = 500L })
        local.users[500L] = TLRPC.TL_user().apply {
            id = 500L
            first_name = "Sam"
        }

        val emitted = repo.observeContacts().first()
        assertEquals(1, emitted.size)
        assertEquals(500L, emitted[0].id)
    }

    @Test
    fun testContainerWiringDefaultsToImpl() {
        val container = AccountFeatureContainer.get(0)
        val repo = container.contactsRepository
        assertTrue(repo is ContactsRepositoryImpl)

        val created = container.createContactsRepository()
        assertTrue(created is ContactsRepositoryImpl)
    }
}
