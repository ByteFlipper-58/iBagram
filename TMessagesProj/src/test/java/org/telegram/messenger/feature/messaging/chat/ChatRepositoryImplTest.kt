package org.telegram.messenger.feature.messaging.chat

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chat.data.datasource.ChatLocalDataSource
import org.telegram.messenger.feature.messaging.chat.data.datasource.ChatRemoteDataSource
import org.telegram.messenger.feature.messaging.chat.data.repository.ChatRepositoryImpl
import org.telegram.messenger.feature.messaging.chat.domain.model.MessageModel

class ChatRepositoryImplTest {

    private lateinit var localDataSource: ChatLocalDataSource
    private lateinit var remoteDataSource: ChatRemoteDataSource
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = ChatLocalDataSource(0)
        remoteDataSource = ChatRemoteDataSource(0)
        repository = ChatRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testGetMessagesInitiallyEmpty() = runBlocking {
        val res = repository.getMessages(100L)
        assertTrue(res is Result.Success)
        val list = (res as Result.Success).data
        assertTrue(list.isEmpty())
    }

    @Test
    fun testSetAndGetMessages() = runBlocking {
        val msg = MessageModel(
            id = 1,
            dialogId = 100L,
            senderId = 200L,
            text = "Hello World",
            date = 123456,
            isOut = false,
            isUnread = true
        )
        localDataSource.setMessages(100L, listOf(msg))

        val res = repository.getMessages(100L)
        assertTrue(res is Result.Success)
        val list = (res as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("Hello World", list[0].text)

        val observed = repository.observeMessages(100L).first()
        assertEquals(1, observed.size)
        assertEquals(1, observed[0].id)
    }

    @Test
    fun testSendMessage() = runBlocking {
        val res = repository.sendMessage(100L, "New message")
        assertTrue(res is Result.Success)

        val messages = repository.observeMessages(100L).first()
        assertEquals(1, messages.size)
        assertEquals("New message", messages[0].text)
        assertTrue(messages[0].isOut)
    }

    @Test
    fun testDeleteMessages() = runBlocking {
        val msg1 = MessageModel(id = 1, dialogId = 100L, senderId = 200L, text = "Msg 1", date = 123456, isOut = false, isUnread = false)
        val msg2 = MessageModel(id = 2, dialogId = 100L, senderId = 200L, text = "Msg 2", date = 123456, isOut = false, isUnread = false)
        localDataSource.setMessages(100L, listOf(msg1, msg2))

        val res = repository.deleteMessages(100L, listOf(1), true)
        assertTrue(res is Result.Success)

        val messages = repository.observeMessages(100L).first()
        assertEquals(1, messages.size)
        assertEquals(2, messages[0].id)
    }

    @Test
    fun testLoadHistory() = runBlocking {
        val res = repository.loadHistory(100L, 30)
        assertTrue(res is Result.Success)
    }
}
