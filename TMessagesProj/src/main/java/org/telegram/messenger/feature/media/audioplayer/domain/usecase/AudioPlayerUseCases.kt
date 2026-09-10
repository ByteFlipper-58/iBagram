package org.telegram.messenger.feature.media.audioplayer.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackState
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackStatus
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.media.audioplayer.domain.model.RepeatMode
import org.telegram.messenger.feature.media.audioplayer.domain.repository.AudioPlayerRepository

class ObservePlaybackStateUseCase(
    private val repository: AudioPlayerRepository
) {
    operator fun invoke(): Flow<AudioPlaybackState> = repository.observePlaybackState()
}

class GetPlaybackStateUseCase(
    private val repository: AudioPlayerRepository
) {
    operator fun invoke(): AudioPlaybackState = repository.getPlaybackState()
}

class PlayTrackUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend operator fun invoke(
        track: AudioTrackModel,
        playlist: List<AudioTrackModel> = listOf(track)
    ) {
        val targetPlaylist = if (playlist.none { it.id == track.id }) {
            listOf(track) + playlist
        } else {
            playlist
        }
        repository.play(track, targetPlaylist)
    }
}

class TogglePlayPauseUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend operator fun invoke() {
        val state = repository.getPlaybackState()
        when (state.status) {
            AudioPlaybackStatus.PLAYING -> repository.pause()
            AudioPlaybackStatus.PAUSED -> repository.resume()
            AudioPlaybackStatus.IDLE, AudioPlaybackStatus.ERROR -> {
                val track = state.currentTrack ?: state.playlist.firstOrNull()
                if (track != null) {
                    repository.play(track, state.playlist.ifEmpty { listOf(track) })
                }
            }
            AudioPlaybackStatus.PREPARING, AudioPlaybackStatus.SEEKING -> {
                repository.pause()
            }
        }
    }
}

class SeekAudioUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend fun seekToPosition(positionMs: Long) {
        val state = repository.getPlaybackState()
        val clampedPosition = positionMs.coerceIn(0L, state.durationMs.coerceAtLeast(0L))
        repository.seekTo(clampedPosition)
    }

    suspend fun seekToProgress(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        repository.seekToProgress(clampedProgress)
    }
}

class NavigatePlaylistUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend fun next(): Boolean {
        return repository.next()
    }

    suspend fun previous(): Boolean {
        val state = repository.getPlaybackState()
        // If track played for more than 3 seconds, rewind to beginning first (standard media player rule)
        if (state.currentPositionMs > 3000L && state.status == AudioPlaybackStatus.PLAYING) {
            repository.seekTo(0L)
            return true
        }
        return repository.previous()
    }
}

class CyclePlaybackSpeedUseCase(
    private val repository: AudioPlayerRepository
) {
    companion object {
        val SPEED_STEPS = listOf(0.5f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f)
    }

    suspend fun cycleNext() {
        val state = repository.getPlaybackState()
        val currentSpeed = (Math.round(state.playbackSpeed * 10f) / 10f)
        val nextSpeed = when {
            currentSpeed < 1.0f -> 1.0f
            currentSpeed < 1.2f -> 1.2f
            currentSpeed < 1.5f -> 1.5f
            currentSpeed < 1.8f -> 1.8f
            currentSpeed < 2.0f -> 2.0f
            else -> 0.5f
        }
        repository.setPlaybackSpeed(nextSpeed)
    }

    suspend fun setSpeed(speed: Float) {
        val clampedSpeed = (Math.round(speed.coerceIn(0.5f, 2.5f) * 10f) / 10f)
        repository.setPlaybackSpeed(clampedSpeed)
    }
}

class CycleRepeatModeUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend operator fun invoke(): RepeatMode {
        return repository.toggleRepeatMode()
    }

    suspend fun setMode(mode: RepeatMode) {
        repository.setRepeatMode(mode)
    }
}

class ToggleShuffleUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.toggleShuffle()
    }
}

class HandleProximitySensorUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend operator fun invoke(isNear: Boolean) {
        val state = repository.getPlaybackState()
        repository.setProximityNear(isNear)

        if (isNear) {
            // Switch audio output to earpiece for voice notes or video messages
            if (state.isVoiceOrRoundVideo) {
                repository.setOutputRoute(AudioOutputRoute.EARPIECE)
            }
        } else {
            // Restore speaker route when user pulls device away from ear
            if (state.outputRoute == AudioOutputRoute.EARPIECE) {
                repository.setOutputRoute(AudioOutputRoute.SPEAKER)
            }
        }
    }
}

class ConfigureEqualizerUseCase(
    private val repository: AudioPlayerRepository
) {
    suspend fun setEnabled(enabled: Boolean) {
        repository.setEqualizerEnabled(enabled)
    }

    suspend fun setBandGain(bandIndex: Int, gainMilliBels: Int) {
        repository.setEqualizerBandGain(bandIndex, gainMilliBels)
    }

    suspend fun setBassBoost(strength: Int) {
        repository.setBassBoost(strength.coerceIn(0, 1000))
    }
}
