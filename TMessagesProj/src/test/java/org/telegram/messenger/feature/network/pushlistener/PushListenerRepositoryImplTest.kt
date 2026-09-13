package org.telegram.messenger.feature.network.pushlistener

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.network.pushlistener.data.datasource.PushListenerLocalDataSource
import org.telegram.messenger.feature.network.pushlistener.data.datasource.PushListenerRemoteDataSource
import org.telegram.messenger.feature.network.pushlistener.data.repository.PushListenerRepositoryImpl
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushActionType
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushDecryptStatus
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType

class PushListenerRepositoryImplTest {

    private lateinit var localDataSource: PushListenerLocalDataSource
    private lateinit var remoteDataSource: PushListenerRemoteDataSource
    private lateinit var repository: PushListenerRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = PushListenerLocalDataSource(account = 0)
        remoteDataSource = PushListenerRemoteDataSource(account = 0)
        repository = PushListenerRepositoryImpl(
            account = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testInitialState() = runTest {
        val state = repository.getState()
        assertTrue(state.isListening)
        assertEquals(0, state.totalReceivedPushes)
        assertEquals(0, state.totalDecryptErrors)
        assertNull(state.lastProcessedPush)
    }

    @Test
    fun testToggleListening() = runTest {
        repository.setListening(false)
        assertFalse(repository.getState().isListening)

        repository.setListening(true)
        assertTrue(repository.getState().isListening)
    }

    @Test
    fun testProcessPush_whenDisabled() = runTest {
        repository.setListening(false)

        val result = repository.processPush(
            pushType = PushType.FIREBASE,
            rawData = "{\"loc_key\":\"CHAT_MESSAGE\"}",
            timestamp = 1000L
        )

        assertFalse(result.isHandled)
        assertEquals("Push listener is disabled", result.errorMessage)
    }

    @Test
    fun testProcessPush_emptyPayload() = runTest {
        val result = repository.processPush(
            pushType = PushType.FIREBASE,
            rawData = "",
            timestamp = 1000L
        )

        assertFalse(result.isHandled)
        assertEquals(PushDecryptStatus.PAYLOAD_CORRUPTED, result.status)
        assertEquals(1, repository.getState().totalDecryptErrors)
    }

    @Test
    fun testProcessPush_success() = runTest {
        val raw = """{"loc_key":"CHAT_MESSAGE","chat_id":12345,"msg_id":42}"""
        val result = repository.processPush(
            pushType = PushType.FIREBASE,
            rawData = raw,
            timestamp = 1234567L
        )

        assertTrue(result.isHandled)
        assertEquals(PushDecryptStatus.SUCCESS, result.status)
        assertNotNull(result.payload)
        assertEquals(PushActionType.NEW_MESSAGE, result.payload?.actionType)
        assertEquals(12345L, result.payload?.chatId)
        assertEquals(42, result.payload?.messageId)

        val state = repository.getState()
        assertEquals(1, state.totalReceivedPushes)
        assertEquals(result.payload, state.lastProcessedPush)
    }

    @Test
    fun testRegisterToken() = runTest {
        repository.registerToken(PushType.FIREBASE, "fcm_sample_token_123")
        val state = repository.getState()
        assertEquals("fcm_sample_token_123", state.registeredTokens[PushType.FIREBASE])

        repository.registerToken(PushType.HUAWEI, "hcm_sample_token_456")
        val updatedState = repository.getState()
        assertEquals("hcm_sample_token_456", updatedState.registeredTokens[PushType.HUAWEI])
    }

    @Test
    fun testReportDecryptError() = runTest {
        repository.reportDecryptError(PushType.FIREBASE)
        val state = repository.getState()
        assertEquals(1, state.totalDecryptErrors)
        assertNotNull(state.lastError)
    }

    @Test
    fun testClearHistory() = runTest {
        repository.processPush(
            pushType = PushType.FIREBASE,
            rawData = """{"loc_key":"CHAT_MESSAGE"}""",
            timestamp = 1000L
        )
        assertEquals(1, repository.getState().totalReceivedPushes)

        repository.clearHistory()
        val cleared = repository.getState()
        assertEquals(0, cleared.totalReceivedPushes)
        assertNull(cleared.lastProcessedPush)
    }
}
