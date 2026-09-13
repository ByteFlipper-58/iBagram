package org.telegram.messenger.feature.messaging.ephemeralmessages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.datasource.EphemeralMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.datasource.EphemeralMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.ephemeralmessages.data.repository.EphemeralMessagesRepositoryImpl

class EphemeralMessagesRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeEphemeralMessagesRemoteDataSource
    private lateinit var fakeLocalDataSource: FakeEphemeralMessagesLocalDataSource
    private lateinit var repository: EphemeralMessagesRepositoryImpl

    private class FakeEphemeralMessagesRemoteDataSource : EphemeralMessagesRemoteDataSource(0)

    private class FakeEphemeralMessagesLocalDataSource : EphemeralMessagesLocalDataSource(0)

    @Before
    fun setup() {
        fakeRemoteDataSource = FakeEphemeralMessagesRemoteDataSource()
        fakeLocalDataSource = FakeEphemeralMessagesLocalDataSource()
        repository = EphemeralMessagesRepositoryImpl(
            currentAccount = 0,
            localDataSource = fakeLocalDataSource,
            remoteDataSource = fakeRemoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testParseCommand() {
        val cmd1 = repository.parseCommand("/start")
        assertNotNull(cmd1)
        assertEquals("start", cmd1?.command)
        assertNull(cmd1?.botUsername)

        val cmd2 = repository.parseCommand("/help@test_bot with args")
        assertNotNull(cmd2)
        assertEquals("help", cmd2?.command)
        assertEquals("test_bot", cmd2?.botUsername)

        val invalid = repository.parseCommand("regular message")
        assertNull(invalid)

        val slashOnly = repository.parseCommand("/")
        assertNull(slashOnly)
    }

    @Test
    fun testAnchorBindingsCrud() = runBlocking {
        val dialogId = 12345L

        repository.clearAllAnchorBindings()
        assertTrue(repository.getAnchorBindings(dialogId).isEmpty())

        repository.putAnchorBinding(dialogId, 100, 200)
        val bindings1 = repository.getAnchorBindings(dialogId)
        assertEquals(1, bindings1.size)
        assertEquals(200, bindings1[100])

        repository.putAnchorBinding(dialogId, 101, 201)
        val bindings2 = repository.getAnchorBindings(dialogId)
        assertEquals(2, bindings2.size)

        repository.removeAnchorBinding(dialogId, 100, 200)
        val bindings3 = repository.getAnchorBindings(dialogId)
        assertEquals(1, bindings3.size)
        assertEquals(201, bindings3[101])

        repository.clearAnchorBindings(dialogId)
        assertTrue(repository.getAnchorBindings(dialogId).isEmpty())
    }

    @Test
    fun testObserveState() = runBlocking {
        repository.clearAllAnchorBindings()
        repository.putAnchorBinding(555L, 10, 20)

        val state = repository.observeState().first()
        val dialogBindings = state.activeAnchorBindings[555L]
        assertNotNull(dialogBindings)
        assertEquals(20, dialogBindings?.get(10))
    }

    @Test
    fun testEphemeralCommandDetection() {
        fakeLocalDataSource.registerTestBotCommand("secret", "safe_bot", 999L, true)
        fakeLocalDataSource.registerTestBotCommand("normal", null, 888L, false)

        val botId = repository.getEphemeralCommandBotId("/secret@safe_bot", 111L)
        assertEquals(999L, botId)
        assertTrue(repository.isEphemeralCommand("/secret@safe_bot", 111L))

        val normalBotId = repository.getEphemeralCommandBotId("/normal", 111L)
        assertEquals(0L, normalBotId)
        assertFalse(repository.isEphemeralCommand("/normal", 111L))
    }
}
