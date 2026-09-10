package org.telegram.messenger.feature.social.contacts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.contacts.data.mapper.ContactMapper
import org.telegram.messenger.feature.social.contacts.domain.model.ContactModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository
import org.telegram.messenger.feature.social.contacts.domain.usecase.AddContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.DeleteContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.ObserveContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.SearchContactsUseCase
import org.telegram.messenger.feature.social.contacts.presentation.ContactsEvent
import org.telegram.messenger.feature.social.contacts.presentation.ContactsUiState
import org.telegram.messenger.feature.social.contacts.presentation.ContactsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeContactsRepository : ContactsRepository {
        val contactsMap = mutableMapOf<Long, ContactModel>()
        val contactsFlow = MutableSharedFlow<List<ContactModel>>(replay = 1)
        var shouldSucceed = true

        fun putContact(contact: ContactModel) {
            contactsMap[contact.id] = contact
            contactsFlow.tryEmit(contactsMap.values.toList())
        }

        fun removeContact(id: Long) {
            contactsMap.remove(id)
            contactsFlow.tryEmit(contactsMap.values.toList())
        }

        override fun observeContacts(): Flow<List<ContactModel>> = contactsFlow.asSharedFlow()

        override suspend fun getContacts(forceReload: Boolean): List<ContactModel> = contactsMap.values.toList()

        override suspend fun getContact(userId: Long): ContactModel? = contactsMap[userId]

        override suspend fun addContact(
            userId: Long,
            firstName: String,
            lastName: String,
            phone: String
        ): Result<Unit> {
            return if (shouldSucceed) {
                val contact = ContactModel(
                    id = userId,
                    firstName = firstName,
                    lastName = lastName,
                    displayName = "$firstName $lastName".trim(),
                    username = "",
                    phone = phone,
                    isMutual = false,
                    isOnline = false
                )
                putContact(contact)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to add contact"))
            }
        }

        override suspend fun deleteContact(userId: Long): Result<Unit> {
            return if (shouldSucceed) {
                if (contactsMap.containsKey(userId)) {
                    removeContact(userId)
                    Result.success(Unit)
                } else {
                    Result.failure(AppError.NotFound("Contact not found"))
                }
            } else {
                Result.failure(AppError.Generic("Failed to delete contact"))
            }
        }

        override suspend fun searchContacts(query: String): List<ContactModel> {
            if (query.isBlank()) return contactsMap.values.toList()
            val q = query.lowercase().trim()
            return contactsMap.values.filter {
                it.displayName.lowercase().contains(q) ||
                    it.firstName.lowercase().contains(q) ||
                    it.lastName.lowercase().contains(q) ||
                    it.username.lowercase().contains(q) ||
                    it.phone.contains(q)
            }
        }
    }

    @Test
    fun testContactMapper() {
        val contact = TLRPC.TL_contact().apply {
            user_id = 12345L
            mutual = true
        }
        val user = TLRPC.TL_user().apply {
            id = 12345L
            first_name = "Pavel"
            last_name = "Durov"
            username = "durov"
            phone = "+971500000000"
            status = TLRPC.TL_userStatusOnline().apply { expires = 1700000000 }
        }

        val domainModel = ContactMapper.toDomain(contact, user)

        assertEquals(12345L, domainModel.id)
        assertEquals("Pavel", domainModel.firstName)
        assertEquals("Durov", domainModel.lastName)
        assertEquals("durov", domainModel.username)
        assertEquals("+971500000000", domainModel.phone)
        assertTrue(domainModel.isMutual)
        assertTrue(domainModel.isOnline)
        assertTrue(domainModel.displayName.contains("Pavel"))
    }

    @Test
    fun testContactMapperWithNullUser() {
        val contact = TLRPC.TL_contact().apply {
            user_id = 999L
            mutual = false
        }
        val domainModel = ContactMapper.toDomain(contact, null)

        assertEquals(999L, domainModel.id)
        assertEquals("", domainModel.firstName)
        assertEquals("", domainModel.lastName)
        assertEquals("", domainModel.displayName)
        assertFalse(domainModel.isMutual)
        assertFalse(domainModel.isOnline)
    }

    @Test
    fun testContactMapperList() {
        val contact1 = TLRPC.TL_contact().apply { user_id = 1L }
        val contact2 = TLRPC.TL_contact().apply { user_id = 2L }

        val user1 = TLRPC.TL_user().apply {
            id = 1L
            first_name = "Alice"
        }
        val user2 = TLRPC.TL_user().apply {
            id = 2L
            first_name = "Bob"
        }

        val usersMap = mapOf(1L to user1, 2L to user2)
        val domainList = ContactMapper.toDomainList(listOf(contact1, contact2)) { usersMap[it] }

        assertEquals(2, domainList.size)
        assertEquals("Alice", domainList[0].firstName)
        assertEquals("Bob", domainList[1].firstName)
    }

    @Test
    fun testObserveAndGetContactsUseCases() = runTest {
        val repo = FakeContactsRepository()
        val c1 = ContactModel(1L, "Alice", "Smith", "Alice Smith", "asmith", "+111", true, true)
        val c2 = ContactModel(2L, "Bob", "Jones", "Bob Jones", "bjones", "+222", false, false)

        repo.putContact(c1)
        repo.putContact(c2)

        val observeUseCase = ObserveContactsUseCase(repo)
        val getContactsUseCase = GetContactsUseCase(repo)
        val getContactUseCase = GetContactUseCase(repo)

        val contactsList = getContactsUseCase()
        assertEquals(2, contactsList.size)

        val singleContact = getContactUseCase(1L)
        assertNotNull(singleContact)
        assertEquals("Alice", singleContact?.firstName)

        val notFound = getContactUseCase(999L)
        assertNull(notFound)

        var emitted: List<ContactModel>? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            observeUseCase().collect { emitted = it }
        }

        advanceUntilIdle()
        assertNotNull(emitted)
        assertEquals(2, emitted?.size)

        job.cancel()
    }

    @Test
    fun testAddAndDeleteContactUseCases() = runTest {
        val repo = FakeContactsRepository()
        val addUseCase = AddContactUseCase(repo)
        val deleteUseCase = DeleteContactUseCase(repo)

        val addResult = addUseCase(3L, "Charlie", "Brown", "+333")
        assertTrue(addResult.isSuccess)
        assertEquals(1, repo.getContacts().size)

        val deleteResult = deleteUseCase(3L)
        assertTrue(deleteResult.isSuccess)
        assertEquals(0, repo.getContacts().size)

        // Error path
        repo.shouldSucceed = false
        val failAdd = addUseCase(4L, "Fail", "User", "+000")
        assertTrue(failAdd.isFailure)

        val failDelete = deleteUseCase(4L)
        assertTrue(failDelete.isFailure)
    }

    @Test
    fun testSearchContactsUseCase() = runTest {
        val repo = FakeContactsRepository()
        repo.putContact(ContactModel(1L, "Alice", "Wonderland", "Alice Wonderland", "alice", "+123", true, true))
        repo.putContact(ContactModel(2L, "Bob", "Builder", "Bob Builder", "bob", "+456", false, false))
        repo.putContact(ContactModel(3L, "Charlie", "Chaplin", "Charlie Chaplin", "charlie", "+789", true, false))

        val searchUseCase = SearchContactsUseCase(repo)

        assertEquals(3, searchUseCase("").size)
        assertEquals(1, searchUseCase("alice").size)
        assertEquals(1, searchUseCase("+456").size)
        assertEquals(1, searchUseCase("Chaplin").size)
        assertEquals(0, searchUseCase("nonexistent").size)
    }

    @Test
    fun testContactsViewModelFlowAndEvents() = runTest {
        val repo = FakeContactsRepository()
        val c1 = ContactModel(1L, "Alice", "Smith", "Alice Smith", "alice", "+111", true, true)
        val c2 = ContactModel(2L, "Bob", "Jones", "Bob Jones", "bob", "+222", false, false)
        repo.putContact(c1)
        repo.putContact(c2)

        val viewModel = ContactsViewModel(
            observeContactsUseCase = ObserveContactsUseCase(repo),
            getContactsUseCase = GetContactsUseCase(repo),
            getContactUseCase = GetContactUseCase(repo),
            addContactUseCase = AddContactUseCase(repo),
            deleteContactUseCase = DeleteContactUseCase(repo),
            searchContactsUseCase = SearchContactsUseCase(repo)
        )

        val events = mutableListOf<ContactsEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()

        // Verify initial state
        assertTrue(viewModel.uiState.value is ContactsUiState.Success)
        val state = viewModel.uiState.value as ContactsUiState.Success
        assertEquals(2, state.contacts.size)
        assertEquals("", state.searchQuery)
        assertEquals(2, state.displayedContacts.size)

        // Search query
        viewModel.search("alice")
        advanceUntilIdle()
        val searchState = viewModel.uiState.value as ContactsUiState.Success
        assertEquals("alice", searchState.searchQuery)
        assertEquals(1, searchState.searchResults.size)
        assertEquals(1, searchState.displayedContacts.size)
        assertEquals("Alice", searchState.displayedContacts.first().firstName)

        // Clear search
        viewModel.search("")
        advanceUntilIdle()
        val clearedState = viewModel.uiState.value as ContactsUiState.Success
        assertEquals("", clearedState.searchQuery)
        assertEquals(2, clearedState.displayedContacts.size)

        // Add contact
        viewModel.addContact(3L, "David", "Copperfield", "+999")
        advanceUntilIdle()
        assertTrue(events.any { it is ContactsEvent.ContactAdded && it.userId == 3L })

        // Delete contact
        viewModel.deleteContact(1L)
        advanceUntilIdle()
        assertTrue(events.any { it is ContactsEvent.ContactDeleted && it.userId == 1L })

        // Refresh
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ContactsUiState.Success)

        eventsJob.cancel()
    }

    @Test
    fun testContactsViewModelErrorHandling() = runTest {
        val repo = FakeContactsRepository().apply { shouldSucceed = false }

        val viewModel = ContactsViewModel(
            observeContactsUseCase = ObserveContactsUseCase(repo),
            getContactsUseCase = GetContactsUseCase(repo),
            getContactUseCase = GetContactUseCase(repo),
            addContactUseCase = AddContactUseCase(repo),
            deleteContactUseCase = DeleteContactUseCase(repo),
            searchContactsUseCase = SearchContactsUseCase(repo)
        )

        val events = mutableListOf<ContactsEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.addContact(999L, "Error", "User", "+000")
        advanceUntilIdle()
        assertTrue(events.any { it is ContactsEvent.ShowError && it.message == "Failed to add contact" })

        viewModel.deleteContact(999L)
        advanceUntilIdle()
        assertTrue(events.any { it is ContactsEvent.ShowError && it.message == "Failed to delete contact" })

        eventsJob.cancel()
    }

    @Test
    fun testAccountFeatureContainerWiring() {
        val container = AccountFeatureContainer.get(0)
        val customRepo = FakeContactsRepository()
        container.contactsRepository = customRepo

        assertEquals(customRepo, container.contactsRepository)
        assertNotNull(container.observeContactsUseCase)
        assertNotNull(container.getContactsUseCase)
        assertNotNull(container.getContactUseCase)
        assertNotNull(container.addContactUseCase)
        assertNotNull(container.deleteContactUseCase)
        assertNotNull(container.searchContactsUseCase)
        assertNotNull(container.contactsViewModel)

        AccountFeatureContainer.reset(0)
    }
}
