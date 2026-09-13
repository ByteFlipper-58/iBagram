package org.telegram.messenger.feature.business.businessrecipients

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.business.businessrecipients.data.datasource.BusinessRecipientsLocalDataSource
import org.telegram.messenger.feature.business.businessrecipients.data.repository.BusinessRecipientsRepositoryImpl
import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType

class BusinessRecipientsRepositoryImplTest {

    private lateinit var localDataSource: BusinessRecipientsLocalDataSource
    private lateinit var repository: BusinessRecipientsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = BusinessRecipientsLocalDataSource()
        repository = BusinessRecipientsRepositoryImpl(localDataSource)
    }

    @Test
    fun `initial state is default BusinessRecipientsModel`() {
        val model = repository.getRecipients()
        assertTrue(model.excludeSelected)
        assertTrue(model.selectedUserIds.isEmpty())
        assertTrue(model.excludedUserIds.isEmpty())
    }

    @Test
    fun `toggleExcludeSelected toggles flag`() {
        repository.toggleExcludeSelected(true)
        assertTrue(repository.getRecipients().excludeSelected)

        repository.toggleExcludeSelected(false)
        assertFalse(repository.getRecipients().excludeSelected)
    }

    @Test
    fun `toggleFilter enables and disables individual filters`() {
        repository.toggleFilter(RecipientFilterType.CONTACTS, true)
        assertTrue(repository.getRecipients().contacts)

        repository.toggleFilter(RecipientFilterType.NEW_CHATS, true)
        assertTrue(repository.getRecipients().newChats)

        repository.toggleFilter(RecipientFilterType.CONTACTS, false)
        assertFalse(repository.getRecipients().contacts)
        assertTrue(repository.getRecipients().newChats)
    }

    @Test
    fun `addSelectedUsers and removeSelectedUser manage selection list`() {
        repository.addSelectedUsers(listOf(101L, 102L, 103L))
        assertEquals(setOf(101L, 102L, 103L), repository.getRecipients().selectedUserIds)

        repository.removeSelectedUser(102L)
        assertEquals(setOf(101L, 103L), repository.getRecipients().selectedUserIds)
    }

    @Test
    fun `addExcludedUsers and removeExcludedUser manage exclusion list`() {
        repository.addExcludedUsers(listOf(201L, 202L))
        assertEquals(setOf(201L, 202L), repository.getRecipients().excludedUserIds)

        repository.removeExcludedUser(201L)
        assertEquals(setOf(202L), repository.getRecipients().excludedUserIds)
    }

    @Test
    fun `adding to selected removes from excluded and vice versa`() {
        repository.addExcludedUsers(listOf(500L, 501L))
        assertTrue(repository.getRecipients().excludedUserIds.contains(500L))

        // Adding 500L to selected should remove it from excluded
        repository.addSelectedUsers(listOf(500L))
        assertTrue(repository.getRecipients().selectedUserIds.contains(500L))
        assertFalse(repository.getRecipients().excludedUserIds.contains(500L))

        // Adding 500L to excluded should remove it from selected
        repository.addExcludedUsers(listOf(500L))
        assertTrue(repository.getRecipients().excludedUserIds.contains(500L))
        assertFalse(repository.getRecipients().selectedUserIds.contains(500L))
    }

    @Test
    fun `hasChanges detects differences from initial model`() {
        val initial = BusinessRecipientsModel(contacts = true)
        val current = BusinessRecipientsModel(contacts = false)

        assertTrue(repository.hasChanges(initial, current))
        assertFalse(repository.hasChanges(initial, initial))
    }

    @Test
    fun `reset restores default empty state`() {
        localDataSource.setRecipients(
            BusinessRecipientsModel(
                excludeSelected = false,
                selectedUserIds = setOf(1L, 2L),
                contacts = true
            )
        )

        repository.reset()
        val res = repository.getRecipients()
        assertTrue(res.excludeSelected)
        assertTrue(res.selectedUserIds.isEmpty())
        assertFalse(res.contacts)
    }
}
