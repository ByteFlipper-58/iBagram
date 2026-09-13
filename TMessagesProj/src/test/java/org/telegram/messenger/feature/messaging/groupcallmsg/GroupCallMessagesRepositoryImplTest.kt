package org.telegram.messenger.feature.messaging.groupcallmsg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource.GroupCallMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.groupcallmsg.data.datasource.GroupCallMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.groupcallmsg.data.repository.GroupCallMessagesRepositoryImpl
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.model.GroupCallMessageModel

class GroupCallMessagesRepositoryImplTest {

    private lateinit var localDataSource: GroupCallMessagesLocalDataSource
    private lateinit var remoteDataSource: GroupCallMessagesRemoteDataSource
    private lateinit var repository: GroupCallMessagesRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = GroupCallMessagesLocalDataSource(account = 0)
        remoteDataSource = GroupCallMessagesRemoteDataSource(account = 0)
        repository = GroupCallMessagesRepositoryImpl(
            account = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testInitialMessagesEmpty() = runTest {
        val state = repository.getCallMessages(callId = 12345L)
        assertEquals(12345L, state.callId)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun testSendCallMessage() = runTest {
        val callId = 9999L
        val peerId = 1234L
        val text = "Hello in-call message"

        repository.sendCallMessage(callId, peerId, text)

        val state = repository.getCallMessages(callId)
        assertEquals(1, state.messages.size)
        val msg = state.messages.first()
        assertEquals(peerId, msg.fromId)
        assertEquals(text, msg.text)
        assertTrue(msg.isOut)
        assertTrue(msg.isSuccessful)
    }

    @Test
    fun testPopMessage() = runTest {
        val callId = 8888L
        repository.sendCallMessage(callId, 1L, "First")
        repository.sendCallMessage(callId, 2L, "Second")

        assertEquals(2, repository.getCallMessages(callId).messages.size)

        repository.popMessage(callId)
        val state = repository.getCallMessages(callId)
        assertEquals(1, state.messages.size)
        assertEquals("Second", state.messages.first().text)
    }

    @Test
    fun testClearCallMessages() = runTest {
        val callId = 7777L
        repository.sendCallMessage(callId, 1L, "Msg 1")
        repository.sendCallMessage(callId, 2L, "Msg 2")
        assertEquals(2, repository.getCallMessages(callId).messages.size)

        repository.clearCallMessages(callId)
        val state = repository.getCallMessages(callId)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun testAddLocalDirectly() = runTest {
        val callId = 5555L
        val msg = GroupCallMessageModel(
            randomId = 101L,
            fromId = 500L,
            text = "Direct text"
        )
        localDataSource.addCallMessage(callId, msg)

        val state = repository.getCallMessages(callId)
        assertEquals(1, state.messages.size)
        assertEquals(101L, state.messages.first().randomId)
    }
}
