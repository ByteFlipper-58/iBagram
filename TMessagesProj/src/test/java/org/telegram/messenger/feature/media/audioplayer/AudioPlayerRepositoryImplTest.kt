package org.telegram.messenger.feature.media.audioplayer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.audioplayer.data.datasource.AudioPlayerLocalDataSource
import org.telegram.messenger.feature.media.audioplayer.data.datasource.AudioPlayerRemoteDataSource
import org.telegram.messenger.feature.media.audioplayer.data.repository.AudioPlayerRepositoryImpl
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackStatus
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.media.audioplayer.domain.model.RepeatMode

class AudioPlayerRepositoryImplTest {

    private lateinit var localDataSource: AudioPlayerLocalDataSource
    private lateinit var remoteDataSource: AudioPlayerRemoteDataSource
    private lateinit var repository: AudioPlayerRepositoryImpl

    private val track1 = AudioTrackModel(
        id = 1L,
        dialogId = 100L,
        messageId = 10,
        title = "Track 1",
        performer = "Artist 1",
        durationMs = 180000L
    )

    private val track2 = AudioTrackModel(
        id = 2L,
        dialogId = 100L,
        messageId = 11,
        title = "Track 2",
        performer = "Artist 2",
        durationMs = 240000L
    )

    @Before
    fun setUp() {
        localDataSource = AudioPlayerLocalDataSource(currentAccount = 0, testMode = true)
        remoteDataSource = AudioPlayerRemoteDataSource(currentAccount = 0)
        repository = AudioPlayerRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialPlaybackState() = runTest {
        val state = repository.getPlaybackState()
        assertEquals(AudioPlaybackStatus.IDLE, state.status)
        assertEquals(0, state.playlist.size)

        val flowState = repository.observePlaybackState().first()
        assertEquals(AudioPlaybackStatus.IDLE, flowState.status)
    }

    @Test
    fun testPlayPauseResumeStop() = runTest {
        val playlist = listOf(track1, track2)
        repository.play(track1, playlist)

        var state = repository.getPlaybackState()
        assertEquals(AudioPlaybackStatus.PLAYING, state.status)
        assertEquals(track1, state.currentTrack)
        assertEquals(2, state.playlist.size)

        repository.pause()
        state = repository.getPlaybackState()
        assertEquals(AudioPlaybackStatus.PAUSED, state.status)

        val resumed = repository.resume()
        assertTrue(resumed)
        state = repository.getPlaybackState()
        assertEquals(AudioPlaybackStatus.PLAYING, state.status)

        repository.stop()
        state = repository.getPlaybackState()
        assertEquals(AudioPlaybackStatus.IDLE, state.status)
        assertEquals(0L, state.currentPositionMs)
    }

    @Test
    fun testSeekOperations() = runTest {
        repository.play(track1, listOf(track1))
        repository.seekTo(60000L)

        var state = repository.getPlaybackState()
        assertEquals(60000L, state.currentPositionMs)
        assertEquals(60000f / 180000f, state.progress, 0.01f)

        repository.seekToProgress(0.5f)
        state = repository.getPlaybackState()
        assertEquals(90000L, state.currentPositionMs)
        assertEquals(0.5f, state.progress, 0.01f)

        repository.updatePosition(120000L)
        state = repository.getPlaybackState()
        assertEquals(120000L, state.currentPositionMs)
    }

    @Test
    fun testPlaylistNavigation() = runTest {
        val playlist = listOf(track1, track2)
        repository.play(track1, playlist)

        val hasNext = repository.next()
        assertTrue(hasNext)
        assertEquals(track2, repository.getPlaybackState().currentTrack)

        val hasPrev = repository.previous()
        assertTrue(hasPrev)
        assertEquals(track1, repository.getPlaybackState().currentTrack)
    }

    @Test
    fun testRepeatAndShuffleModes() = runTest {
        repository.setRepeatMode(RepeatMode.ALL)
        assertEquals(RepeatMode.ALL, repository.getPlaybackState().repeatMode)

        val nextMode = repository.toggleRepeatMode()
        assertEquals(RepeatMode.CURRENT, nextMode)

        val shuffled = repository.toggleShuffle()
        assertTrue(shuffled)
        assertTrue(repository.getPlaybackState().isShuffleEnabled)

        val unshuffled = repository.toggleShuffle()
        assertFalse(unshuffled)
        assertFalse(repository.getPlaybackState().isShuffleEnabled)
    }

    @Test
    fun testEqualizerAndOutputRoute() = runTest {
        repository.setOutputRoute(AudioOutputRoute.HEADPHONES)
        assertEquals(AudioOutputRoute.HEADPHONES, repository.getPlaybackState().outputRoute)

        repository.setProximityNear(true)
        assertTrue(repository.getPlaybackState().isProximityNear)

        repository.setEqualizerEnabled(true)
        assertTrue(repository.getPlaybackState().equalizer.isEnabled)

        repository.setEqualizerBandGain(bandIndex = 0, gainMilliBels = 500)
        val band = repository.getPlaybackState().equalizer.bands.firstOrNull { it.bandIndex == 0 }
        assertNotNull(band)
        assertEquals(500, band?.gainMilliBels)

        repository.setBassBoost(300)
        assertEquals(300, repository.getPlaybackState().equalizer.bassBoostStrength)

        repository.reset()
        assertEquals(AudioPlaybackStatus.IDLE, repository.getPlaybackState().status)
    }

    @Test
    fun testRemoteDataSource() = runTest {
        val configRes = remoteDataSource.fetchAudioPlaybackConfig()
        assertTrue(configRes.isSuccess)

        val eventRes = remoteDataSource.reportAudioPlaybackEvent(100L, 10)
        assertTrue(eventRes.isSuccess)
    }
}
