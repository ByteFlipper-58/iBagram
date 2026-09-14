package org.telegram.messenger.feature.system.ringtones

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.ringtones.data.datasource.RingtoneLocalDataSource
import org.telegram.messenger.feature.system.ringtones.data.datasource.RingtoneRemoteDataSource
import org.telegram.messenger.feature.system.ringtones.data.repository.RingtoneRepositoryImpl
import org.telegram.messenger.feature.system.ringtones.domain.model.RingtoneModel

class RingtoneRepositoryImplTest {

    private lateinit var localDataSource: RingtoneLocalDataSource
    private lateinit var remoteDataSource: RingtoneRemoteDataSource
    private lateinit var repository: RingtoneRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = RingtoneLocalDataSource(currentAccount = 0)
        localDataSource.setTestMode()
        remoteDataSource = RingtoneRemoteDataSource(currentAccount = 0)
        repository = RingtoneRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testAddGetAndRemoveRingtone() = runTest {
        val model = RingtoneModel(
            id = 200L,
            title = "Test Ringtone",
            durationSec = 3,
            sizeBytes = 15000L,
            mimeType = "audio/ogg",
            localUri = "/path/test.ogg"
        )

        repository.addRingtone(model)
        val all = repository.getRingtones()
        assertEquals(1, all.size)
        assertEquals("Test Ringtone", all[0].title)

        val retrieved = repository.getRingtoneById(200L)
        assertNotNull(retrieved)
        assertEquals(200L, retrieved?.id)

        val path = repository.getRingtoneSoundPath(200L)
        assertEquals("/path/test.ogg", path)

        repository.selectRingtone(200L)
        assertEquals(200L, repository.getState().selectedRingtoneId)

        val observed = repository.observeRingtones().first()
        assertEquals(1, observed.size)

        val removed = repository.removeRingtone(200L)
        assertTrue(removed)
        assertTrue(repository.getRingtones().isEmpty())
        assertNull(repository.getState().selectedRingtoneId)
    }

    @Test
    fun testSaveFromDocument() = runTest {
        val saved = repository.saveRingtoneFromDocument(
            documentId = 500L,
            title = "Doc Sound",
            durationSec = 2,
            sizeBytes = 25000L,
            mimeType = "audio/mpeg"
        )
        assertTrue(saved)
        assertEquals(1, repository.getRingtones().size)
        assertEquals(500L, repository.getRingtoneById(500L)?.id)

        // Duplicate save should return true without adding second item
        val duplicate = repository.saveRingtoneFromDocument(
            documentId = 500L,
            title = "Doc Sound",
            durationSec = 2,
            sizeBytes = 25000L,
            mimeType = "audio/mpeg"
        )
        assertTrue(duplicate)
        assertEquals(1, repository.getRingtones().size)
    }

    @Test
    fun testUploadLifecycle() = runTest {
        val uploadRes = repository.uploadRingtone(
            filePath = "/storage/tone.mp3",
            fileName = "tone.mp3",
            durationSec = 4,
            sizeBytes = 40000L
        )
        assertTrue(uploadRes.isSuccess)
        val tone = uploadRes.getOrThrow()
        assertTrue(tone.isUploading)
        assertEquals("tone", tone.title)

        // Complete upload
        localDataSource.completeUpload("/storage/tone.mp3", 9999L)
        val updated = repository.getRingtoneById(9999L)
        assertNotNull(updated)
        assertFalse(updated!!.isUploading)

        // Upload and cancel
        val upload2 = repository.uploadRingtone(
            filePath = "/storage/cancel.ogg",
            fileName = "cancel.ogg",
            durationSec = 3,
            sizeBytes = 30000L
        )
        assertTrue(upload2.isSuccess)
        assertEquals(2, repository.getRingtones().size)

        repository.cancelUpload("/storage/cancel.ogg")
        assertEquals(1, repository.getRingtones().size)
    }

    @Test
    fun testLimitsAndRefresh() = runTest {
        val limits = repository.getLimits()
        assertEquals(5, limits.maxDurationSeconds)
        assertEquals(300 * 1024L, limits.maxSizeBytes)

        repository.refreshRingtones(force = false)
        assertFalse(repository.getState().isLoading)
    }

    @Test
    fun testRemoteDataSourceFallback() = runTest {
        val fetchRes = remoteDataSource.fetchRemoteRingtones()
        assertTrue(fetchRes.isSuccess)

        val saveRes = remoteDataSource.saveRemoteRingtone(100L, 200L)
        assertTrue(saveRes.isSuccess)
    }
}
