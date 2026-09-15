package org.telegram.messenger.feature.messaging.sendmessages

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.sendmessages.data.datasource.SendMessagesLocalDataSource
import org.telegram.messenger.feature.messaging.sendmessages.data.datasource.SendMessagesRemoteDataSource
import org.telegram.messenger.feature.messaging.sendmessages.data.repository.SendMessagesRepositoryImpl
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaType
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendOptionsModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendStatus

class SendMessagesRepositoryImplTest {

    private lateinit var localDataSource: SendMessagesLocalDataSource
    private lateinit var remoteDataSource: SendMessagesRemoteDataSource
    private lateinit var repository: SendMessagesRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = SendMessagesLocalDataSource(0)
        remoteDataSource = SendMessagesRemoteDataSource(0)
        repository = SendMessagesRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testSendTextAddsToPendingSendsAndReturnsModel() = runBlocking {
        val result = repository.sendText(12345L, "Hello world", SendOptionsModel())
        assertEquals("Hello world", result.text)
        assertEquals(12345L, result.dialogId)
        assertEquals(SendMediaType.TEXT, result.type)

        val pending = repository.getPendingSends(12345L)
        assertEquals(1, pending.size)
        assertEquals(result.localId, pending.first().localId)
    }

    @Test
    fun testSendMediaTracksUploadingStatus() = runBlocking {
        val item = SendMediaItem(
            id = "photo_1",
            path = "/sdcard/photo.jpg",
            type = SendMediaType.PHOTO,
            caption = "Nice pic",
            sizeBytes = 2048L
        )
        val result = repository.sendMedia(12345L, item, SendOptionsModel())
        assertEquals(SendStatus.UPLOADING, result.status)
        assertEquals("Nice pic", result.text)
        assertEquals(2048L, result.totalBytes)

        val pending = repository.getPendingSends(12345L)
        assertEquals(1, pending.size)
        assertEquals(result.localId, pending.first().localId)
    }

    @Test
    fun testSendAlbumCreatesMultiplePendingSends() = runBlocking {
        val items = listOf(
            SendMediaItem(id = "1", path = "/sdcard/1.jpg", type = SendMediaType.PHOTO, caption = "One"),
            SendMediaItem(id = "2", path = "/sdcard/2.jpg", type = SendMediaType.PHOTO, caption = "Two")
        )
        val results = repository.sendAlbum(12345L, items, SendOptionsModel())
        assertEquals(2, results.size)
        assertEquals("One", results[0].text)
        assertEquals("Two", results[1].text)

        val pending = repository.getPendingSends(12345L)
        assertEquals(2, pending.size)
    }

    @Test
    fun testForwardMessagesCreatesPendingSends() = runBlocking {
        val request = ForwardRequestModel(
            targetDialogId = 99999L,
            sourceDialogId = 11111L,
            messageIds = listOf(101, 102, 103)
        )
        val results = repository.forwardMessages(request)
        assertEquals(3, results.size)
        assertEquals(99999L, results[0].dialogId)

        val pending = repository.getPendingSends(99999L)
        assertEquals(3, pending.size)
    }

    @Test
    fun testUpdateProgressUpdatesState() = runBlocking {
        val item = repository.sendText(12345L, "Upload test", SendOptionsModel())
        repository.updateProgress(item.localId, 0.5f, 512L)

        val pending = repository.getPendingSends(12345L).first { it.localId == item.localId }
        assertEquals(0.5f, pending.progress, 0.001f)
        assertEquals(512L, pending.uploadedBytes)
        assertEquals(SendStatus.UPLOADING, pending.status)

        repository.updateProgress(item.localId, 1.0f, 1024L)
        val completed = repository.getPendingSends(12345L).first { it.localId == item.localId }
        assertEquals(1.0f, completed.progress, 0.001f)
        assertEquals(SendStatus.SENDING, completed.status)
    }

    @Test
    fun testMarkSuccessAndMarkFailed() = runBlocking {
        val item1 = repository.sendText(12345L, "Msg 1", SendOptionsModel())
        val item2 = repository.sendText(12345L, "Msg 2", SendOptionsModel())

        repository.markSuccess(item1.localId)
        repository.markFailed(item2.localId, "Network error")

        val p1 = repository.getPendingSends(12345L).first { it.localId == item1.localId }
        val p2 = repository.getPendingSends(12345L).first { it.localId == item2.localId }

        assertEquals(SendStatus.SUCCESS, p1.status)
        assertTrue(p1.isFinished)

        assertEquals(SendStatus.FAILED, p2.status)
        assertTrue(p2.isFailed)
        assertEquals("Network error", p2.errorReason)
    }

    @Test
    fun testCancelSendAndRetrySend() = runBlocking {
        val item = repository.sendText(12345L, "To cancel", SendOptionsModel())
        val cancelled = repository.cancelSend(item.localId)
        assertTrue(cancelled)

        val pCancelled = repository.getPendingSends(12345L).first { it.localId == item.localId }
        assertEquals(SendStatus.CANCELLED, pCancelled.status)

        val retried = repository.retrySend(item.localId)
        assertTrue(retried)

        val pRetried = repository.getPendingSends(12345L).first { it.localId == item.localId }
        assertEquals(SendStatus.PENDING, pRetried.status)
        assertEquals(1, pRetried.retryCount)
    }

    @Test
    fun testObservePendingSendsFiltersByDialogId() = runBlocking {
        repository.sendText(100L, "Chat 100", SendOptionsModel())
        repository.sendText(200L, "Chat 200", SendOptionsModel())

        val list100 = repository.observePendingSends(100L).first()
        assertEquals(1, list100.size)
        assertEquals(100L, list100[0].dialogId)

        val listAll = repository.observePendingSends(null).first()
        assertEquals(2, listAll.size)
    }
}
