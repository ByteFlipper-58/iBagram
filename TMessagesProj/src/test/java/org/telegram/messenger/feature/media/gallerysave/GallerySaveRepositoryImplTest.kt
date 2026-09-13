package org.telegram.messenger.feature.media.gallerysave

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.gallerysave.data.datasource.GallerySaveLocalDataSource
import org.telegram.messenger.feature.media.gallerysave.data.datasource.GallerySaveRemoteDataSource
import org.telegram.messenger.feature.media.gallerysave.data.repository.GallerySaveRepositoryImpl
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveDialogExceptionModel
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySavePeerType
import org.telegram.messenger.feature.media.gallerysave.domain.model.GallerySaveTargetSettingsModel

class GallerySaveRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: GallerySaveRemoteDataSource
    private lateinit var fakeLocalDataSource: GallerySaveLocalDataSource
    private lateinit var repository: GallerySaveRepositoryImpl

    @Before
    fun setup() {
        fakeRemoteDataSource = GallerySaveRemoteDataSource(0)
        fakeLocalDataSource = GallerySaveLocalDataSource(0)
        repository = GallerySaveRepositoryImpl(
            account = 0,
            remoteDataSource = fakeRemoteDataSource,
            localDataSource = fakeLocalDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun testInitialConfigObservation() = runBlocking {
        val config = repository.observeConfig().first()
        assertEquals(GallerySavePeerType.PEER, config.userSettings.peerType)
        assertEquals(GallerySavePeerType.GROUP, config.groupSettings.peerType)
        assertEquals(GallerySavePeerType.CHANNEL, config.channelSettings.peerType)
    }

    @Test
    fun testUpdateSettings() = runBlocking {
        val newSettings = GallerySaveTargetSettingsModel(
            peerType = GallerySavePeerType.GROUP,
            savePhoto = true,
            saveVideo = true,
            limitVideoBytes = 50 * 1024 * 1024L
        )

        repository.updateSettings(newSettings)

        val updated = repository.getSettings(GallerySavePeerType.GROUP)
        assertTrue(updated.savePhoto)
        assertTrue(updated.saveVideo)
        assertEquals(50 * 1024 * 1024L, updated.limitVideoBytes)

        val currentConfig = repository.getConfig()
        assertEquals(updated, currentConfig.groupSettings)
    }

    @Test
    fun testTogglePeerType() = runBlocking {
        val initial = repository.getSettings(GallerySavePeerType.PEER)
        assertFalse(initial.savePhoto)
        assertFalse(initial.saveVideo)

        repository.togglePeerType(GallerySavePeerType.PEER)
        val toggledOn = repository.getSettings(GallerySavePeerType.PEER)
        assertTrue(toggledOn.savePhoto)
        assertTrue(toggledOn.saveVideo)

        repository.togglePeerType(GallerySavePeerType.PEER)
        val toggledOff = repository.getSettings(GallerySavePeerType.PEER)
        assertFalse(toggledOff.savePhoto)
        assertFalse(toggledOff.saveVideo)
    }

    @Test
    fun testSetVideoLimitClamping() = runBlocking {
        repository.setVideoLimit(GallerySavePeerType.CHANNEL, 200 * 1024 * 1024L)
        val settings = repository.getSettings(GallerySavePeerType.CHANNEL)
        assertEquals(200 * 1024 * 1024L, settings.limitVideoBytes)

        // Exceeds max 4GB
        repository.setVideoLimit(GallerySavePeerType.CHANNEL, 5L * 1000 * 1024 * 1024)
        val clampedSettings = repository.getSettings(GallerySavePeerType.CHANNEL)
        assertEquals(GallerySaveTargetSettingsModel.MAX_VIDEO_LIMIT_BYTES, clampedSettings.limitVideoBytes)
    }

    @Test
    fun testExceptionsCrud() = runBlocking {
        val ex1 = GallerySaveDialogExceptionModel(
            dialogId = 12345L,
            savePhoto = true,
            saveVideo = false,
            limitVideoBytes = 10 * 1024 * 1024L
        )
        val ex2 = GallerySaveDialogExceptionModel(
            dialogId = 67890L,
            savePhoto = false,
            saveVideo = true,
            limitVideoBytes = 20 * 1024 * 1024L
        )

        repository.setException(GallerySavePeerType.PEER, ex1)
        repository.setException(GallerySavePeerType.PEER, ex2)

        val exceptions = repository.getExceptions(GallerySavePeerType.PEER)
        assertEquals(2, exceptions.size)

        repository.removeException(GallerySavePeerType.PEER, 12345L)
        val remaining = repository.getExceptions(GallerySavePeerType.PEER)
        assertEquals(1, remaining.size)
        assertEquals(67890L, remaining[0].dialogId)

        repository.removeAllExceptions(GallerySavePeerType.PEER)
        assertTrue(repository.getExceptions(GallerySavePeerType.PEER).isEmpty())
    }
}
