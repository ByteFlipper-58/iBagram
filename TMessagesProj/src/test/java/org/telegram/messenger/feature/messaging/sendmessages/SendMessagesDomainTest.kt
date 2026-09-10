package org.telegram.messenger.feature.messaging.sendmessages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.sendmessages.data.mapper.SendMessagesMapper
import org.telegram.messenger.feature.messaging.sendmessages.data.repository.LegacySendMessagesRepository
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardMode
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.ForwardRequestModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaItem
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendMediaType
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendOptionsModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendStatus
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.CancelSendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ForwardMessagesUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ObservePendingSendsUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.RetrySendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaAlbumUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendTextMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.presentation.SendMessagesEvent
import org.telegram.messenger.feature.messaging.sendmessages.presentation.SendMessagesViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessagesDomainTest {

    private lateinit var repository: LegacySendMessagesRepository
    private lateinit var sendTextMessageUseCase: SendTextMessageUseCase
    private lateinit var sendMediaMessageUseCase: SendMediaMessageUseCase
    private lateinit var sendMediaAlbumUseCase: SendMediaAlbumUseCase
    private lateinit var forwardMessagesUseCase: ForwardMessagesUseCase
    private lateinit var retrySendMessageUseCase: RetrySendMessageUseCase
    private lateinit var cancelSendMessageUseCase: CancelSendMessageUseCase
    private lateinit var observePendingSendsUseCase: ObservePendingSendsUseCase

    @Before
    fun setup() {
        repository = LegacySendMessagesRepository(currentAccount = 0)
        sendTextMessageUseCase = SendTextMessageUseCase(repository)
        sendMediaMessageUseCase = SendMediaMessageUseCase(repository)
        sendMediaAlbumUseCase = SendMediaAlbumUseCase(repository)
        forwardMessagesUseCase = ForwardMessagesUseCase(repository)
        retrySendMessageUseCase = RetrySendMessageUseCase(repository)
        cancelSendMessageUseCase = CancelSendMessageUseCase(repository)
        observePendingSendsUseCase = ObservePendingSendsUseCase(repository)
    }

    @Test
    fun testSendTextMessageValidationAndLimits() = runTest {
        // 1. Valid text send
        val pending = sendTextMessageUseCase(
            dialogId = 100L,
            text = "Hello, world!",
            options = SendOptionsModel(notify = false, scheduleDate = 1700000000)
        )
        assertNotNull(pending)
        assertEquals(100L, pending.dialogId)
        assertEquals("Hello, world!", pending.text)
        assertEquals(SendStatus.SENDING, pending.status)
        assertEquals(SendMediaType.TEXT, pending.type)
        assertFalse(pending.options.notify)
        assertTrue(pending.options.isScheduled)

        // 2. Empty text throws
        var emptyException: Exception? = null
        try {
            sendTextMessageUseCase(dialogId = 100L, text = "   ")
        } catch (e: Exception) {
            emptyException = e
        }
        assertNotNull(emptyException)
        assertTrue(emptyException is IllegalArgumentException)

        // 3. Overlength standard text (5000 chars) throws
        val longText = "a".repeat(5000)
        var overlengthException: Exception? = null
        try {
            sendTextMessageUseCase(dialogId = 100L, text = longText, isPremium = false)
        } catch (e: Exception) {
            overlengthException = e
        }
        assertNotNull(overlengthException)
        assertTrue(overlengthException is IllegalArgumentException)

        // 4. Overlength standard text succeeds for Premium (up to 8192 chars)
        val premiumPending = sendTextMessageUseCase(dialogId = 100L, text = longText, isPremium = true)
        assertEquals(longText, premiumPending.text)
    }

    @Test
    fun testSendMediaAlbumValidationAndBatchCreation() = runTest {
        val photo1 = SendMediaItem(id = "p1", path = "/photos/1.jpg", type = SendMediaType.PHOTO, caption = "Album Cover")
        val video2 = SendMediaItem(id = "v2", path = "/videos/2.mp4", type = SendMediaType.VIDEO)
        val photo3 = SendMediaItem(id = "p3", path = "/photos/3.jpg", type = SendMediaType.PHOTO)

        // Valid album of 3 items
        val albumSends = sendMediaAlbumUseCase(dialogId = 200L, items = listOf(photo1, video2, photo3))
        assertEquals(3, albumSends.size)
        assertEquals("Album Cover", albumSends[0].text)
        assertEquals(SendMediaType.PHOTO, albumSends[0].type)
        assertEquals(SendMediaType.VIDEO, albumSends[1].type)
        assertEquals(SendMediaType.PHOTO, albumSends[2].type)

        // Album with > 10 items throws
        val elevenPhotos = (1..11).map {
            SendMediaItem(id = "p$it", path = "/photos/$it.jpg", type = SendMediaType.PHOTO)
        }
        var overlimitException: Exception? = null
        try {
            sendMediaAlbumUseCase(dialogId = 200L, items = elevenPhotos)
        } catch (e: Exception) {
            overlimitException = e
        }
        assertNotNull(overlimitException)
        assertTrue(overlimitException is IllegalArgumentException)

        // Album with non-visual media (e.g. DOCUMENT) throws
        val doc = SendMediaItem(id = "d1", path = "/docs/file.pdf", type = SendMediaType.DOCUMENT)
        var docException: Exception? = null
        try {
            sendMediaAlbumUseCase(dialogId = 200L, items = listOf(photo1, doc))
        } catch (e: Exception) {
            docException = e
        }
        assertNotNull(docException)
        assertTrue(docException is IllegalArgumentException)
    }

    @Test
    fun testForwardMessagesModes() = runTest {
        val request = ForwardRequestModel(
            targetDialogId = 300L,
            sourceDialogId = 400L,
            messageIds = listOf(10, 11, 12),
            mode = ForwardMode.HIDE_NAMES
        )

        val forwards = forwardMessagesUseCase(request)
        assertEquals(3, forwards.size)
        assertEquals(300L, forwards[0].dialogId)
        assertEquals(SendStatus.SENDING, forwards[0].status)

        // Empty message IDs throws
        var emptyException: Exception? = null
        try {
            forwardMessagesUseCase(request.copy(messageIds = emptyList()))
        } catch (e: Exception) {
            emptyException = e
        }
        assertNotNull(emptyException)
        assertTrue(emptyException is IllegalArgumentException)
    }

    @Test
    fun testUploadProgressLifecycleAndCancellation() = runTest {
        val media = SendMediaItem(
            id = "vid1",
            path = "/storage/video.mp4",
            type = SendMediaType.VIDEO,
            sizeBytes = 100000000L // 100MB
        )

        val pending = sendMediaMessageUseCase(dialogId = 500L, item = media)
        assertEquals(SendStatus.UPLOADING, pending.status)
        assertEquals(0f, pending.progress, 0.001f)

        // Update progress to 50%
        repository.updateProgress(pending.localId, 0.5f, 50000000L)
        var current = repository.getPendingSends(500L).first { it.localId == pending.localId }
        assertEquals(SendStatus.UPLOADING, current.status)
        assertEquals(0.5f, current.progress, 0.001f)
        assertEquals(50000000L, current.uploadedBytes)

        // Progress reaches 100% -> state changes to SENDING
        repository.updateProgress(pending.localId, 1.0f, 100000000L)
        current = repository.getPendingSends(500L).first { it.localId == pending.localId }
        assertEquals(SendStatus.SENDING, current.status)
        assertEquals(1.0f, current.progress, 0.001f)

        // Server confirmation -> SUCCESS
        repository.markSuccess(pending.localId)
        current = repository.getPendingSends(500L).first { it.localId == pending.localId }
        assertEquals(SendStatus.SUCCESS, current.status)
        assertTrue(current.isFinished)

        // Cancel another message
        val toCancel = sendTextMessageUseCase(dialogId = 500L, text = "Will cancel")
        val cancelled = cancelSendMessageUseCase(toCancel.localId)
        assertTrue(cancelled)

        val cancelledItem = repository.getPendingSends(500L).first { it.localId == toCancel.localId }
        assertEquals(SendStatus.CANCELLED, cancelledItem.status)
        assertTrue(cancelledItem.isFinished)

        // Mapper formatters
        assertEquals("100.0 MB", SendMessagesMapper.formatFileSize(100000000L + 4857600L))
        assertEquals("1.0 GB", SendMessagesMapper.formatFileSize(1024L * 1024L * 1024L))
    }

    @Test
    fun testRetryFailedMessagesAndViewModelMviEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = SendMessagesViewModel(
            sendTextMessageUseCase = sendTextMessageUseCase,
            sendMediaMessageUseCase = sendMediaMessageUseCase,
            sendMediaAlbumUseCase = sendMediaAlbumUseCase,
            forwardMessagesUseCase = forwardMessagesUseCase,
            retrySendMessageUseCase = retrySendMessageUseCase,
            cancelSendMessageUseCase = cancelSendMessageUseCase,
            observePendingSendsUseCase = observePendingSendsUseCase,
            coroutineScope = testScope
        )

        // 1. Send via ViewModel
        viewModel.onEvent(SendMessagesEvent.SendText(dialogId = 600L, text = "Testing MVI"))
        testDispatcher.scheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertEquals(1, uiState.pendingSends.size)
        assertEquals(0, uiState.failedCount)

        val localId = uiState.pendingSends.first().localId

        // 2. Mark as failed
        repository.markFailed(localId, "Connection reset by peer")
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertEquals(1, uiState.failedCount)
        assertEquals("Connection reset by peer", uiState.pendingSends.first().errorReason)

        // 3. Retry
        viewModel.onEvent(SendMessagesEvent.Retry(localId))
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertEquals(0, uiState.failedCount)
        assertEquals(SendStatus.PENDING, uiState.pendingSends.first().status)
        assertNull(uiState.pendingSends.first().errorReason)
        assertEquals(1, uiState.pendingSends.first().retryCount)

        viewModel.destroy()
    }
}
