package org.telegram.messenger.feature.messaging.dialogs

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.dialogs.data.datasource.DialogsLocalDataSource
import org.telegram.messenger.feature.messaging.dialogs.data.datasource.DialogsRemoteDataSource
import org.telegram.messenger.feature.messaging.dialogs.data.repository.DialogsRepositoryImpl
import org.telegram.messenger.feature.messaging.dialogs.domain.model.DialogModel

class DialogsRepositoryImplTest {

    private lateinit var localDataSource: DialogsLocalDataSource
    private lateinit var remoteDataSource: DialogsRemoteDataSource
    private lateinit var repository: DialogsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = DialogsLocalDataSource(0)
        remoteDataSource = DialogsRemoteDataSource(0)
        repository = DialogsRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testGetDialogsInitiallyEmpty() = runBlocking {
        val list = repository.getDialogs(0).first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testSetAndObserveDialogs() = runBlocking {
        val dialog = DialogModel(
            id = 123L,
            unreadCount = 5,
            lastMessageDate = 1000,
            isPinned = false
        )
        localDataSource.setDialogs(0, listOf(dialog))

        val list = repository.getDialogs(0).first()
        assertEquals(1, list.size)
        assertEquals(123L, list[0].id)
        assertEquals(5, list[0].unreadCount)
    }

    @Test
    fun testPinDialog() = runBlocking {
        val dialog = DialogModel(
            id = 456L,
            isPinned = false
        )
        localDataSource.setDialogs(0, listOf(dialog))

        val result = repository.pinDialog(456L, true)
        assertTrue(result is Result.Success)

        val list = repository.getDialogs(0).first()
        assertTrue(list[0].isPinned)
    }

    @Test
    fun testDeleteDialog() = runBlocking {
        val dialog1 = DialogModel(id = 1L)
        val dialog2 = DialogModel(id = 2L)
        localDataSource.setDialogs(0, listOf(dialog1, dialog2))

        val result = repository.deleteDialog(1L, true)
        assertTrue(result is Result.Success)

        val list = repository.getDialogs(0).first()
        assertEquals(1, list.size)
        assertEquals(2L, list[0].id)
    }

    @Test
    fun testMarkAsRead() = runBlocking {
        val dialog = DialogModel(id = 789L, unreadCount = 10)
        localDataSource.setDialogs(0, listOf(dialog))

        val result = repository.markAsRead(789L)
        assertTrue(result is Result.Success)

        val list = repository.getDialogs(0).first()
        assertEquals(0, list[0].unreadCount)
    }

    @Test
    fun testLoadMoreDialogs() = runBlocking {
        val result = repository.loadMoreDialogs(0, 20)
        assertTrue(result is Result.Success)
    }
}
