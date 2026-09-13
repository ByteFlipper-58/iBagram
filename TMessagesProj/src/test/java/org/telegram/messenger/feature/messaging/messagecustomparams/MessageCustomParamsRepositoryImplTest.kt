package org.telegram.messenger.feature.messaging.messagecustomparams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource.MessageCustomParamsLocalDataSource
import org.telegram.messenger.feature.messaging.messagecustomparams.data.datasource.MessageCustomParamsRemoteDataSource
import org.telegram.messenger.feature.messaging.messagecustomparams.data.repository.MessageCustomParamsRepositoryImpl
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageSummaryParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.VoiceTranscriptionParamsModel

class MessageCustomParamsRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: MessageCustomParamsRemoteDataSource
    private lateinit var fakeLocalDataSource: MessageCustomParamsLocalDataSource
    private lateinit var repository: MessageCustomParamsRepositoryImpl

    @Before
    fun setup() {
        fakeRemoteDataSource = MessageCustomParamsRemoteDataSource(0)
        fakeLocalDataSource = MessageCustomParamsLocalDataSource(0)
        repository = MessageCustomParamsRepositoryImpl(
            currentAccount = 0,
            remoteDataSource = fakeRemoteDataSource,
            localDataSource = fakeLocalDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testSetAndGetParams() = runBlocking {
        val params = MessageCustomParamsModel(
            messageId = 1001L,
            voiceTranscription = VoiceTranscriptionParamsModel(
                text = "Hello world transcription",
                isOpen = true,
                isFinal = true
            ),
            summary = MessageSummaryParamsModel(
                summaryText = "Brief AI summary",
                isSummarizedOpen = true
            )
        )

        repository.setParamsForMessage(1001L, params)

        val retrieved = repository.getParamsForMessage(1001L)
        assertNotNull(retrieved)
        assertEquals("Hello world transcription", retrieved?.voiceTranscription?.text)
        assertEquals("Brief AI summary", retrieved?.summary?.summaryText)

        val state = repository.observeState().first()
        assertEquals(1, state.cachedParamsCount)
        assertEquals(1001L, state.lastUpdatedMessageId)
    }

    @Test
    fun testCopyParams() = runBlocking {
        val original = MessageCustomParamsModel(
            messageId = 500L,
            voiceTranscription = VoiceTranscriptionParamsModel(text = "Transcribed speech")
        )
        repository.setParamsForMessage(500L, original)

        repository.copyParams(500L, 501L)

        val copied = repository.getParamsForMessage(501L)
        assertNotNull(copied)
        assertEquals(501L, copied?.messageId)
        assertEquals("Transcribed speech", copied?.voiceTranscription?.text)

        val state = repository.getState()
        assertEquals(2, state.cachedParamsCount)
        assertEquals(501L, state.lastUpdatedMessageId)
    }

    @Test
    fun testRemoveParams() = runBlocking {
        val p1 = MessageCustomParamsModel(messageId = 1L)
        val p2 = MessageCustomParamsModel(messageId = 2L)
        repository.setParamsForMessage(1L, p1)
        repository.setParamsForMessage(2L, p2)

        assertEquals(2, repository.getState().cachedParamsCount)

        repository.removeParams(1L)
        assertNull(repository.getParamsForMessage(1L))
        assertNotNull(repository.getParamsForMessage(2L))
        assertEquals(1, repository.getState().cachedParamsCount)
    }

    @Test
    fun testClearAll() = runBlocking {
        repository.setParamsForMessage(1L, MessageCustomParamsModel(messageId = 1L))
        repository.setParamsForMessage(2L, MessageCustomParamsModel(messageId = 2L))

        repository.clearAll()
        assertNull(repository.getParamsForMessage(1L))
        assertNull(repository.getParamsForMessage(2L))

        val state = repository.observeState().first()
        assertEquals(0, state.cachedParamsCount)
        assertEquals(0L, state.lastUpdatedMessageId)
    }
}
