package org.telegram.messenger.feature.audioplayer.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

class AudioPlayerViewModel(
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val getPlaybackStateUseCase: GetPlaybackStateUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val togglePlayPauseUseCase: TogglePlayPauseUseCase,
    private val seekAudioUseCase: SeekAudioUseCase,
    private val navigatePlaylistUseCase: NavigatePlaylistUseCase,
    private val cyclePlaybackSpeedUseCase: CyclePlaybackSpeedUseCase,
    private val cycleRepeatModeUseCase: CycleRepeatModeUseCase,
    private val toggleShuffleUseCase: ToggleShuffleUseCase,
    private val handleProximitySensorUseCase: HandleProximitySensorUseCase,
    private val configureEqualizerUseCase: ConfigureEqualizerUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
) {

    private val _uiState = MutableStateFlow(
        AudioPlayerUiState.fromDomain(getPlaybackStateUseCase())
    )
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    init {
        observePlaybackStateUseCase()
            .onEach { domainState ->
                _uiState.update { current ->
                    AudioPlayerUiState.fromDomain(domainState, isLoading = current.isLoading)
                }
            }
            .launchIn(coroutineScope)
    }

    fun onEvent(event: AudioPlayerEvent) {
        when (event) {
            is AudioPlayerEvent.Play -> {
                coroutineScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    playTrackUseCase(event.track, event.playlist)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            is AudioPlayerEvent.TogglePlayPause -> {
                coroutineScope.launch {
                    togglePlayPauseUseCase()
                }
            }
            is AudioPlayerEvent.Stop -> {
                coroutineScope.launch {
                    togglePlayPauseUseCase() // or stop
                }
            }
            is AudioPlayerEvent.SeekToPosition -> {
                coroutineScope.launch {
                    seekAudioUseCase.seekToPosition(event.positionMs)
                }
            }
            is AudioPlayerEvent.SeekToProgress -> {
                coroutineScope.launch {
                    seekAudioUseCase.seekToProgress(event.progress)
                }
            }
            is AudioPlayerEvent.NextTrack -> {
                coroutineScope.launch {
                    navigatePlaylistUseCase.next()
                }
            }
            is AudioPlayerEvent.PreviousTrack -> {
                coroutineScope.launch {
                    navigatePlaylistUseCase.previous()
                }
            }
            is AudioPlayerEvent.CycleSpeed -> {
                coroutineScope.launch {
                    cyclePlaybackSpeedUseCase.cycleNext()
                }
            }
            is AudioPlayerEvent.SetSpeed -> {
                coroutineScope.launch {
                    cyclePlaybackSpeedUseCase.setSpeed(event.speed)
                }
            }
            is AudioPlayerEvent.ToggleRepeatMode -> {
                coroutineScope.launch {
                    cycleRepeatModeUseCase()
                }
            }
            is AudioPlayerEvent.ToggleShuffle -> {
                coroutineScope.launch {
                    toggleShuffleUseCase()
                }
            }
            is AudioPlayerEvent.SetProximity -> {
                coroutineScope.launch {
                    handleProximitySensorUseCase(event.isNear)
                }
            }
            is AudioPlayerEvent.SetOutputRoute -> {
                // Handled via Proximity or external Bluetooth/Headphone route changes
            }
            is AudioPlayerEvent.ToggleEqualizer -> {
                coroutineScope.launch {
                    configureEqualizerUseCase.setEnabled(event.isEnabled)
                }
            }
            is AudioPlayerEvent.SetBassBoost -> {
                coroutineScope.launch {
                    configureEqualizerUseCase.setBassBoost(event.strength)
                }
            }
        }
    }

    fun destroy() {
        coroutineScope.cancel()
    }
}
