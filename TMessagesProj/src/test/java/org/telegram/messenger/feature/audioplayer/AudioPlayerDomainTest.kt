package org.telegram.messenger.feature.audioplayer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.audioplayer.data.mapper.AudioPlayerMapper
import org.telegram.messenger.feature.audioplayer.data.repository.LegacyAudioPlayerRepository
import org.telegram.messenger.feature.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.audioplayer.domain.model.AudioPlaybackStatus
import org.telegram.messenger.feature.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.audioplayer.domain.model.AudioTrackType
import org.telegram.messenger.feature.audioplayer.domain.model.RepeatMode
import org.telegram.messenger.feature.audioplayer.domain.usecase.ConfigureEqualizerUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.CyclePlaybackSpeedUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.CycleRepeatModeUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.GetPlaybackStateUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.HandleProximitySensorUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.NavigatePlaylistUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.ObservePlaybackStateUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.PlayTrackUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.SeekAudioUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.TogglePlayPauseUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.ToggleShuffleUseCase
import org.telegram.messenger.feature.audioplayer.presentation.AudioPlayerEvent
import org.telegram.messenger.feature.audioplayer.presentation.AudioPlayerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AudioPlayerDomainTest {

    private lateinit var repository: LegacyAudioPlayerRepository
    private lateinit var observePlaybackStateUseCase: ObservePlaybackStateUseCase
    private lateinit var getPlaybackStateUseCase: GetPlaybackStateUseCase
    private lateinit var playTrackUseCase: PlayTrackUseCase
    private lateinit var togglePlayPauseUseCase: TogglePlayPauseUseCase
    private lateinit var seekAudioUseCase: SeekAudioUseCase
    private lateinit var navigatePlaylistUseCase: NavigatePlaylistUseCase
    private lateinit var cyclePlaybackSpeedUseCase: CyclePlaybackSpeedUseCase
    private lateinit var cycleRepeatModeUseCase: CycleRepeatModeUseCase
    private lateinit var toggleShuffleUseCase: ToggleShuffleUseCase
    private lateinit var handleProximitySensorUseCase: HandleProximitySensorUseCase
    private lateinit var configureEqualizerUseCase: ConfigureEqualizerUseCase

    private val track1 = AudioTrackModel(
        id = 101L,
        dialogId = 1L,
        messageId = 10,
        title = "Track One",
        performer = "Artist A",
        durationMs = 180000L,
        type = AudioTrackType.MUSIC
    )

    private val track2 = AudioTrackModel(
        id = 102L,
        dialogId = 1L,
        messageId = 11,
        title = "Track Two",
        performer = "Artist B",
        durationMs = 240000L,
        type = AudioTrackType.MUSIC
    )

    private val track3 = AudioTrackModel(
        id = 103L,
        dialogId = 1L,
        messageId = 12,
        title = "Voice Message",
        performer = "Contact C",
        durationMs = 45000L,
        type = AudioTrackType.VOICE
    )

    @Before
    fun setup() {
        repository = LegacyAudioPlayerRepository(currentAccount = 0)
        observePlaybackStateUseCase = ObservePlaybackStateUseCase(repository)
        getPlaybackStateUseCase = GetPlaybackStateUseCase(repository)
        playTrackUseCase = PlayTrackUseCase(repository)
        togglePlayPauseUseCase = TogglePlayPauseUseCase(repository)
        seekAudioUseCase = SeekAudioUseCase(repository)
        navigatePlaylistUseCase = NavigatePlaylistUseCase(repository)
        cyclePlaybackSpeedUseCase = CyclePlaybackSpeedUseCase(repository)
        cycleRepeatModeUseCase = CycleRepeatModeUseCase(repository)
        toggleShuffleUseCase = ToggleShuffleUseCase(repository)
        handleProximitySensorUseCase = HandleProximitySensorUseCase(repository)
        configureEqualizerUseCase = ConfigureEqualizerUseCase(repository)
    }

    @Test
    fun testPlaybackLifecycleAndStateTransitions() = runTest {
        assertEquals(AudioPlaybackStatus.IDLE, getPlaybackStateUseCase().status)

        // 1. Play track1
        playTrackUseCase(track1, listOf(track1, track2))
        var state = getPlaybackStateUseCase()
        assertEquals(AudioPlaybackStatus.PLAYING, state.status)
        assertEquals(track1, state.currentTrack)
        assertEquals(180000L, state.durationMs)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0, state.currentIndex)

        // 2. Pause
        togglePlayPauseUseCase()
        state = getPlaybackStateUseCase()
        assertEquals(AudioPlaybackStatus.PAUSED, state.status)

        // 3. Resume
        togglePlayPauseUseCase()
        state = getPlaybackStateUseCase()
        assertEquals(AudioPlaybackStatus.PLAYING, state.status)

        // 4. Stop
        repository.stop()
        state = getPlaybackStateUseCase()
        assertEquals(AudioPlaybackStatus.IDLE, state.status)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testSeekPositionClampingAndProgressMath() = runTest {
        playTrackUseCase(track1)

        // Seek within bounds
        seekAudioUseCase.seekToPosition(45000L)
        var state = getPlaybackStateUseCase()
        assertEquals(45000L, state.currentPositionMs)
        assertEquals(0.25f, state.progress, 0.001f)

        // Seek negative -> clamped to 0
        seekAudioUseCase.seekToPosition(-5000L)
        state = getPlaybackStateUseCase()
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0f, state.progress, 0.001f)

        // Seek over duration -> clamped to duration
        seekAudioUseCase.seekToPosition(999999L)
        state = getPlaybackStateUseCase()
        assertEquals(180000L, state.currentPositionMs)
        assertEquals(1.0f, state.progress, 0.001f)

        // Seek by normalized progress float (0.5f)
        seekAudioUseCase.seekToProgress(0.5f)
        state = getPlaybackStateUseCase()
        assertEquals(90000L, state.currentPositionMs)
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun testPlaylistNavigationAndRepeatModes() = runTest {
        val playlist = listOf(track1, track2)
        playTrackUseCase(track1, playlist)

        // Mode NONE: track1 -> next() -> track2
        navigatePlaylistUseCase.next()
        var state = getPlaybackStateUseCase()
        assertEquals(track2, state.currentTrack)
        assertEquals(1, state.currentIndex)

        // Next at end in RepeatMode.NONE -> stops
        navigatePlaylistUseCase.next()
        state = getPlaybackStateUseCase()
        assertEquals(AudioPlaybackStatus.IDLE, state.status)

        // Set RepeatMode.ALL and restart
        cycleRepeatModeUseCase.setMode(RepeatMode.ALL)
        playTrackUseCase(track2, playlist)
        navigatePlaylistUseCase.next()
        state = getPlaybackStateUseCase()
        assertEquals(track1, state.currentTrack) // Wrapped to beginning!
        assertEquals(0, state.currentIndex)

        // Set RepeatMode.CURRENT
        cycleRepeatModeUseCase.setMode(RepeatMode.CURRENT)
        navigatePlaylistUseCase.next()
        state = getPlaybackStateUseCase()
        assertEquals(track1, state.currentTrack) // Repeats current!

        // Test Previous track rewind rule:
        // Position > 3000ms -> rewinds to 0ms
        repository.updatePosition(15000L)
        navigatePlaylistUseCase.previous()
        state = getPlaybackStateUseCase()
        assertEquals(0L, state.currentPositionMs)
        assertEquals(track1, state.currentTrack)
    }

    @Test
    fun testSpeedCyclingAndFormatTimecode() = runTest {
        playTrackUseCase(track1)
        assertEquals(1.0f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        // Step through speeds: 1.0f -> 1.2f -> 1.5f -> 1.8f -> 2.0f -> 0.5f -> 1.0f
        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(1.2f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(1.5f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(1.8f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(2.0f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(0.5f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        cyclePlaybackSpeedUseCase.cycleNext()
        assertEquals(1.0f, getPlaybackStateUseCase().playbackSpeed, 0.01f)

        // Timecode formatting
        assertEquals("00:00", AudioPlayerMapper.formatTimeMs(0L))
        assertEquals("00:05", AudioPlayerMapper.formatTimeMs(5000L))
        assertEquals("01:15", AudioPlayerMapper.formatTimeMs(75000L))
        assertEquals("01:05:30", AudioPlayerMapper.formatTimeMs(3930000L))
    }

    @Test
    fun testProximitySensorRoutingAndViewModelMviEvents() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = AudioPlayerViewModel(
            observePlaybackStateUseCase = observePlaybackStateUseCase,
            getPlaybackStateUseCase = getPlaybackStateUseCase,
            playTrackUseCase = playTrackUseCase,
            togglePlayPauseUseCase = togglePlayPauseUseCase,
            seekAudioUseCase = seekAudioUseCase,
            navigatePlaylistUseCase = navigatePlaylistUseCase,
            cyclePlaybackSpeedUseCase = cyclePlaybackSpeedUseCase,
            cycleRepeatModeUseCase = cycleRepeatModeUseCase,
            toggleShuffleUseCase = toggleShuffleUseCase,
            handleProximitySensorUseCase = handleProximitySensorUseCase,
            configureEqualizerUseCase = configureEqualizerUseCase,
            coroutineScope = testScope
        )

        // Play voice message track
        viewModel.onEvent(AudioPlayerEvent.Play(track3))
        testDispatcher.scheduler.advanceUntilIdle()

        var uiState = viewModel.uiState.value
        assertEquals(AudioPlaybackStatus.PLAYING, uiState.status)
        assertEquals(AudioOutputRoute.SPEAKER, uiState.outputRoute)
        assertEquals("00:45", uiState.formattedDuration)

        // Proximity sensor near ear -> switches route to EARPIECE for voice note
        viewModel.onEvent(AudioPlayerEvent.SetProximity(isNear = true))
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertTrue(uiState.isProximityNear)
        assertEquals(AudioOutputRoute.EARPIECE, uiState.outputRoute)

        // Proximity sensor moved away -> restores SPEAKER
        viewModel.onEvent(AudioPlayerEvent.SetProximity(isNear = false))
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertFalse(uiState.isProximityNear)
        assertEquals(AudioOutputRoute.SPEAKER, uiState.outputRoute)

        // Toggle Repeat & Shuffle
        viewModel.onEvent(AudioPlayerEvent.ToggleRepeatMode)
        viewModel.onEvent(AudioPlayerEvent.ToggleShuffle)
        testDispatcher.scheduler.advanceUntilIdle()

        uiState = viewModel.uiState.value
        assertEquals(RepeatMode.ALL, uiState.repeatMode)
        assertTrue(uiState.isShuffleEnabled)

        viewModel.destroy()
    }
}
