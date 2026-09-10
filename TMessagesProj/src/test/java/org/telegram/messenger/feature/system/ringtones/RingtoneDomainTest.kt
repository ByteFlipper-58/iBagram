package org.telegram.messenger.feature.system.ringtones

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
import org.telegram.messenger.feature.system.ringtones.data.mapper.RingtoneMapper
import org.telegram.messenger.feature.system.ringtones.data.repository.LegacyRingtoneRepository
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneErrorCode
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel
import org.telegram.messenger.feature.system.ringtones.domain.usecase.AddRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.CancelRingtoneUploadUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtoneByIdUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtoneSoundPathUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ObserveRingtoneStateUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ObserveRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RefreshRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RemoveRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SaveRingtoneFromDocumentUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SelectRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.UploadRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ValidateRingtoneEligibilityUseCase
import org.telegram.messenger.feature.system.ringtones.presentation.RingtoneEvent
import org.telegram.messenger.feature.system.ringtones.presentation.RingtoneViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class RingtoneDomainTest {

    private lateinit var repository: LegacyRingtoneRepository
    private lateinit var validateEligibilityUseCase: ValidateRingtoneEligibilityUseCase
    private lateinit var observeRingtonesUseCase: ObserveRingtonesUseCase
    private lateinit var observeStateUseCase: ObserveRingtoneStateUseCase
    private lateinit var getRingtonesUseCase: GetRingtonesUseCase
    private lateinit var getRingtoneByIdUseCase: GetRingtoneByIdUseCase
    private lateinit var getRingtoneSoundPathUseCase: GetRingtoneSoundPathUseCase
    private lateinit var addRingtoneUseCase: AddRingtoneUseCase
    private lateinit var removeRingtoneUseCase: RemoveRingtoneUseCase
    private lateinit var saveFromDocumentUseCase: SaveRingtoneFromDocumentUseCase
    private lateinit var uploadRingtoneUseCase: UploadRingtoneUseCase
    private lateinit var cancelUploadUseCase: CancelRingtoneUploadUseCase
    private lateinit var refreshRingtonesUseCase: RefreshRingtonesUseCase
    private lateinit var selectRingtoneUseCase: SelectRingtoneUseCase

    @Before
    fun setUp() {
        repository = LegacyRingtoneRepository(currentAccount = 0)
        validateEligibilityUseCase = ValidateRingtoneEligibilityUseCase()
        observeRingtonesUseCase = ObserveRingtonesUseCase(repository)
        observeStateUseCase = ObserveRingtoneStateUseCase(repository)
        getRingtonesUseCase = GetRingtonesUseCase(repository)
        getRingtoneByIdUseCase = GetRingtoneByIdUseCase(repository)
        getRingtoneSoundPathUseCase = GetRingtoneSoundPathUseCase(repository)
        addRingtoneUseCase = AddRingtoneUseCase(repository, validateEligibilityUseCase)
        removeRingtoneUseCase = RemoveRingtoneUseCase(repository)
        saveFromDocumentUseCase = SaveRingtoneFromDocumentUseCase(repository, validateEligibilityUseCase)
        uploadRingtoneUseCase = UploadRingtoneUseCase(repository, validateEligibilityUseCase)
        cancelUploadUseCase = CancelRingtoneUploadUseCase(repository)
        refreshRingtonesUseCase = RefreshRingtonesUseCase(repository)
        selectRingtoneUseCase = SelectRingtoneUseCase(repository)
    }

    @Test
    fun testRingtoneValidationRules() {
        // Valid audio file (3s, 100KB, ogg)
        val valid1 = validateEligibilityUseCase(
            durationSec = 3,
            sizeBytes = 100 * 1024L,
            mimeType = "audio/ogg",
            fileExtension = "ogg"
        )
        assertTrue(valid1.isValid)
        assertEquals(RingtoneErrorCode.NONE, valid1.errorCode)

        // Valid audio file (5s, 300KB, mp3)
        val valid2 = validateEligibilityUseCase(
            durationSec = 5,
            sizeBytes = 300 * 1024L,
            mimeType = "audio/mpeg",
            fileExtension = "mp3"
        )
        assertTrue(valid2.isValid)
        assertEquals(RingtoneErrorCode.NONE, valid2.errorCode)

        // Invalid duration (> 5 seconds)
        val tooLong = validateEligibilityUseCase(
            durationSec = 6,
            sizeBytes = 100 * 1024L,
            mimeType = "audio/ogg",
            fileExtension = "ogg"
        )
        assertFalse(tooLong.isValid)
        assertEquals(RingtoneErrorCode.TOO_LONG, tooLong.errorCode)

        // Invalid size (> 300 KB)
        val tooBig = validateEligibilityUseCase(
            durationSec = 4,
            sizeBytes = 301 * 1024L,
            mimeType = "audio/ogg",
            fileExtension = "ogg"
        )
        assertFalse(tooBig.isValid)
        assertEquals(RingtoneErrorCode.TOO_BIG, tooBig.errorCode)

        // Invalid format (.wav)
        val unsupported = validateEligibilityUseCase(
            durationSec = 3,
            sizeBytes = 100 * 1024L,
            mimeType = "audio/wav",
            fileExtension = "wav"
        )
        assertFalse(unsupported.isValid)
        assertEquals(RingtoneErrorCode.UNSUPPORTED_FORMAT, unsupported.errorCode)
    }

    @Test
    fun testRingtoneMapperFormattingAndMimeResolution() {
        // Duration formatting
        assertEquals("0:03", RingtoneMapper.formatDuration(3))
        assertEquals("0:05", RingtoneMapper.formatDuration(5))
        assertEquals("1:05", RingtoneMapper.formatDuration(65))

        // File size formatting (with Locale.US)
        assertEquals("500 B", RingtoneMapper.formatFileSize(500L))
        assertEquals("100.0 KB", RingtoneMapper.formatFileSize(100L * 1024L))
        assertEquals("2.0 MB", RingtoneMapper.formatFileSize(2L * 1024L * 1024L))

        // MIME type resolution
        assertEquals("audio/ogg", RingtoneMapper.resolveMimeTypeFromExtension("ogg"))
        assertEquals("audio/mpeg", RingtoneMapper.resolveMimeTypeFromExtension("mp3"))
        assertEquals("audio/m4a", RingtoneMapper.resolveMimeTypeFromExtension("m4a"))
        assertEquals("audio/mpeg", RingtoneMapper.resolveMimeTypeFromExtension("unknown"))

        // Title extraction
        assertEquals("notification chime", RingtoneMapper.extractTitleFromFileName("notification_chime.mp3"))
        assertEquals("custom sound", RingtoneMapper.extractTitleFromFileName("custom-sound.ogg"))
        assertEquals("Sound", RingtoneMapper.extractTitleFromFileName(".mp3"))
    }

    @Test
    fun testAddQueryAndRemoveRingtones() {
        val tone1 = RingtoneModel(
            id = 101L,
            title = "Bell",
            durationSec = 2,
            sizeBytes = 50 * 1024L,
            mimeType = "audio/ogg",
            localUri = "/storage/bell.ogg"
        )
        val tone2 = RingtoneModel(
            id = 102L,
            title = "Chime",
            durationSec = 4,
            sizeBytes = 120 * 1024L,
            mimeType = "audio/mpeg",
            localUri = "/storage/chime.mp3"
        )

        // Add
        val result1 = addRingtoneUseCase(tone1)
        assertTrue(result1.isValid)
        val result2 = addRingtoneUseCase(tone2)
        assertTrue(result2.isValid)

        // Query
        val all = getRingtonesUseCase()
        assertEquals(2, all.size)

        val retrieved1 = getRingtoneByIdUseCase(101L)
        assertNotNull(retrieved1)
        assertEquals("Bell", retrieved1?.title)

        // Sound path
        assertEquals("/storage/bell.ogg", getRingtoneSoundPathUseCase(101L))
        assertNull(getRingtoneSoundPathUseCase(999L))

        // Select tone1
        selectRingtoneUseCase(101L)
        assertEquals(101L, repository.getState().selectedRingtoneId)

        // Remove tone1 -> selected becomes null
        assertTrue(removeRingtoneUseCase(101L))
        assertEquals(1, getRingtonesUseCase().size)
        assertNull(repository.getState().selectedRingtoneId)
        assertFalse(removeRingtoneUseCase(101L)) // already removed
    }

    @Test
    fun testUploadLifecycleAndCancellation() {
        // Upload valid file
        val uploadResult = uploadRingtoneUseCase(
            filePath = "/storage/music/ring.mp3",
            fileName = "ring.mp3",
            durationSec = 3,
            sizeBytes = 80 * 1024L
        )
        assertTrue(uploadResult.isSuccess)
        val pendingTone = uploadResult.getOrThrow()
        assertTrue(pendingTone.isUploading)
        assertEquals("ring", pendingTone.title)

        val state = repository.getState()
        assertEquals(1, state.ringtones.size)
        assertTrue(state.ringtones[0].isUploading)

        // Complete upload
        repository.completeUpload("/storage/music/ring.mp3", 5555L)
        val updated = getRingtoneByIdUseCase(5555L)
        assertNotNull(updated)
        assertFalse(updated!!.isUploading)
        assertEquals(5555L, updated.id)

        // Upload another and cancel
        val upload2 = uploadRingtoneUseCase(
            filePath = "/storage/music/cancel_me.ogg",
            fileName = "cancel_me.ogg",
            durationSec = 4,
            sizeBytes = 90 * 1024L
        )
        assertTrue(upload2.isSuccess)
        assertEquals(2, getRingtonesUseCase().size)

        cancelUploadUseCase("/storage/music/cancel_me.ogg")
        assertEquals(1, getRingtonesUseCase().size)
        assertNull(getRingtoneByIdUseCase(upload2.getOrThrow().id))
    }

    @Test
    fun testViewModelMviStateAndEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = RingtoneViewModel(
            observeRingtoneStateUseCase = observeStateUseCase,
            refreshRingtonesUseCase = refreshRingtonesUseCase,
            selectRingtoneUseCase = selectRingtoneUseCase,
            removeRingtoneUseCase = removeRingtoneUseCase,
            saveRingtoneFromDocumentUseCase = saveFromDocumentUseCase,
            uploadRingtoneUseCase = uploadRingtoneUseCase,
            cancelRingtoneUploadUseCase = cancelUploadUseCase,
            scope = testScope
        )

        testScheduler.advanceUntilIdle()
        var uiState = viewModel.uiState.value
        assertEquals(0, uiState.ringtones.size)
        assertNull(uiState.selectedRingtoneId)
        assertNull(uiState.previewPlayingId)

        // 1. Save tone from document
        viewModel.onEvent(
            RingtoneEvent.SaveFromDocument(
                documentId = 701L,
                title = "Telegram Ping",
                durationSec = 2,
                sizeBytes = 45 * 1024L,
                mimeType = "audio/ogg"
            )
        )
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(1, uiState.ringtones.size)
        assertEquals("Telegram Ping", uiState.ringtones[0].title)

        // 2. Select ringtone
        viewModel.onEvent(RingtoneEvent.SelectRingtone(701L))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(701L, uiState.selectedRingtoneId)

        // 3. Toggle preview
        viewModel.onEvent(RingtoneEvent.TogglePreview(701L))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(701L, uiState.previewPlayingId)

        viewModel.onEvent(RingtoneEvent.TogglePreview(701L))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertNull(uiState.previewPlayingId)

        // 4. Upload invalid file -> error in UI state
        viewModel.onEvent(
            RingtoneEvent.UploadFile(
                filePath = "/storage/too_long.mp3",
                fileName = "too_long.mp3",
                durationSec = 10, // > 5s
                sizeBytes = 50 * 1024L
            )
        )
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("exceeds max 5s"))

        // Clear error
        viewModel.onEvent(RingtoneEvent.ClearError)
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertNull(uiState.error)

        // 5. Delete ringtone
        viewModel.onEvent(RingtoneEvent.DeleteRingtone(701L))
        testScheduler.advanceUntilIdle()
        uiState = viewModel.uiState.value
        assertEquals(0, uiState.ringtones.size)
        assertNull(uiState.selectedRingtoneId)
    }
}
