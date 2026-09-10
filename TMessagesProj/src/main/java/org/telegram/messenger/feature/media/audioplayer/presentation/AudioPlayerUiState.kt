package org.telegram.messenger.feature.media.audioplayer.presentation

import org.telegram.messenger.feature.media.audioplayer.data.mapper.AudioPlayerMapper
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackState
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackStatus
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.media.audioplayer.domain.model.RepeatMode

data class AudioPlayerUiState(
    val currentTrack: AudioTrackModel? = null,
    val status: AudioPlaybackStatus = AudioPlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val formattedPosition: String = "00:00",
    val formattedDuration: String = "00:00",
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val isShuffleEnabled: Boolean = false,
    val outputRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val isProximityNear: Boolean = false,
    val playlist: List<AudioTrackModel> = emptyList(),
    val currentIndex: Int = -1,
    val isEqualizerEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isPlaying: Boolean
        get() = status == AudioPlaybackStatus.PLAYING

    val isPaused: Boolean
        get() = status == AudioPlaybackStatus.PAUSED

    companion object {
        fun fromDomain(state: AudioPlaybackState, isLoading: Boolean = false): AudioPlayerUiState {
            return AudioPlayerUiState(
                currentTrack = state.currentTrack,
                status = state.status,
                currentPositionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                progress = state.progress,
                formattedPosition = AudioPlayerMapper.formatTimeMs(state.currentPositionMs),
                formattedDuration = AudioPlayerMapper.formatTimeMs(state.durationMs),
                playbackSpeed = state.playbackSpeed,
                repeatMode = state.repeatMode,
                isShuffleEnabled = state.isShuffleEnabled,
                outputRoute = state.outputRoute,
                isProximityNear = state.isProximityNear,
                playlist = state.playlist,
                currentIndex = state.currentIndex,
                isEqualizerEnabled = state.equalizer.isEnabled,
                bassBoostStrength = state.equalizer.bassBoostStrength,
                isLoading = isLoading,
                errorMessage = state.errorMessage
            )
        }
    }
}
