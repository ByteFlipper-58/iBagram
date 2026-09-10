package org.telegram.messenger.feature.audioplayer.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.audioplayer.data.mapper.AudioPlayerMapper
import org.telegram.messenger.feature.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.audioplayer.domain.model.AudioPlaybackState
import org.telegram.messenger.feature.audioplayer.domain.model.AudioPlaybackStatus
import org.telegram.messenger.feature.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.audioplayer.domain.model.EqualizerBand
import org.telegram.messenger.feature.audioplayer.domain.model.RepeatMode
import org.telegram.messenger.feature.audioplayer.domain.repository.AudioPlayerRepository

class LegacyAudioPlayerRepository(
    private val currentAccount: Int = 0
) : AudioPlayerRepository {

    private val _state = MutableStateFlow(
        AudioPlaybackState(
            equalizer = org.telegram.messenger.feature.audioplayer.domain.model.EqualizerState(
                bands = listOf(
                    EqualizerBand(0, 60, 0),
                    EqualizerBand(1, 230, 0),
                    EqualizerBand(2, 910, 0),
                    EqualizerBand(3, 3600, 0),
                    EqualizerBand(4, 14000, 0)
                )
            )
        )
    )

    private var shuffleOrder: List<Int> = emptyList()

    override fun observePlaybackState(): Flow<AudioPlaybackState> = _state.asStateFlow()

    override fun getPlaybackState(): AudioPlaybackState = _state.value

    override suspend fun play(
        track: AudioTrackModel,
        playlist: List<AudioTrackModel>
    ) {
        val targetPlaylist = if (playlist.isEmpty()) listOf(track) else playlist
        val trackIndex = targetPlaylist.indexOfFirst { it.id == track.id }.let {
            if (it == -1) 0 else it
        }

        if (_state.value.isShuffleEnabled) {
            regenerateShuffleOrder(targetPlaylist.size, trackIndex)
        }

        _state.update { current ->
            current.copy(
                currentTrack = track,
                status = AudioPlaybackStatus.PLAYING,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
                progress = 0f,
                playlist = targetPlaylist,
                currentIndex = trackIndex,
                errorMessage = null
            )
        }
    }

    override suspend fun resume(): Boolean {
        val current = _state.value
        if (current.currentTrack == null) {
            return false
        }
        _state.update { it.copy(status = AudioPlaybackStatus.PLAYING) }
        return true
    }

    override suspend fun pause() {
        _state.update { it.copy(status = AudioPlaybackStatus.PAUSED) }
    }

    override suspend fun stop() {
        _state.update {
            it.copy(
                status = AudioPlaybackStatus.IDLE,
                currentPositionMs = 0L,
                progress = 0f
            )
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        _state.update { current ->
            val clampedPos = positionMs.coerceIn(0L, current.durationMs.coerceAtLeast(0L))
            val progress = AudioPlayerMapper.calculateProgress(clampedPos, current.durationMs)
            current.copy(
                currentPositionMs = clampedPos,
                progress = progress
            )
        }
    }

    override suspend fun seekToProgress(progress: Float) {
        _state.update { current ->
            val clampedProgress = progress.coerceIn(0f, 1f)
            val pos = AudioPlayerMapper.calculatePosition(clampedProgress, current.durationMs)
            current.copy(
                currentPositionMs = pos,
                progress = clampedProgress
            )
        }
    }

    override suspend fun next(): Boolean {
        val current = _state.value
        val nextIndex = AudioPlayerMapper.resolveNextTrackIndex(
            currentIndex = current.currentIndex,
            playlistSize = current.playlist.size,
            repeatMode = current.repeatMode,
            isShuffle = current.isShuffleEnabled,
            shuffleIndices = shuffleOrder
        )

        return if (nextIndex != null && nextIndex in current.playlist.indices) {
            val nextTrack = current.playlist[nextIndex]
            _state.update {
                it.copy(
                    currentTrack = nextTrack,
                    currentIndex = nextIndex,
                    currentPositionMs = 0L,
                    durationMs = nextTrack.durationMs,
                    progress = 0f,
                    status = AudioPlaybackStatus.PLAYING
                )
            }
            true
        } else {
            stop()
            false
        }
    }

    override suspend fun previous(): Boolean {
        val current = _state.value
        val prevIndex = AudioPlayerMapper.resolvePreviousTrackIndex(
            currentIndex = current.currentIndex,
            playlistSize = current.playlist.size,
            repeatMode = current.repeatMode,
            isShuffle = current.isShuffleEnabled,
            shuffleIndices = shuffleOrder
        )

        return if (prevIndex != null && prevIndex in current.playlist.indices) {
            val prevTrack = current.playlist[prevIndex]
            _state.update {
                it.copy(
                    currentTrack = prevTrack,
                    currentIndex = prevIndex,
                    currentPositionMs = 0L,
                    durationMs = prevTrack.durationMs,
                    progress = 0f,
                    status = AudioPlaybackStatus.PLAYING
                )
            }
            true
        } else {
            seekTo(0L)
            false
        }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        val clamped = AudioPlayerMapper.clampSpeed(speed)
        _state.update { it.copy(playbackSpeed = clamped) }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }

    override suspend fun toggleRepeatMode(): RepeatMode {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.CURRENT
            RepeatMode.CURRENT -> RepeatMode.NONE
        }
        _state.update { it.copy(repeatMode = nextMode) }
        return nextMode
    }

    override suspend fun toggleShuffle(): Boolean {
        val nextShuffle = !_state.value.isShuffleEnabled
        if (nextShuffle) {
            val current = _state.value
            regenerateShuffleOrder(current.playlist.size, current.currentIndex)
        } else {
            shuffleOrder = emptyList()
        }
        _state.update { it.copy(isShuffleEnabled = nextShuffle) }
        return nextShuffle
    }

    override suspend fun setOutputRoute(route: AudioOutputRoute) {
        _state.update { it.copy(outputRoute = route) }
    }

    override suspend fun setProximityNear(isNear: Boolean) {
        _state.update { it.copy(isProximityNear = isNear) }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        _state.update { current ->
            current.copy(equalizer = current.equalizer.copy(isEnabled = enabled))
        }
    }

    override suspend fun setEqualizerBandGain(
        bandIndex: Int,
        gainMilliBels: Int
    ) {
        _state.update { current ->
            val updatedBands = current.equalizer.bands.map { band ->
                if (band.bandIndex == bandIndex) {
                    band.copy(gainMilliBels = gainMilliBels.coerceIn(band.minGainMilliBels, band.maxGainMilliBels))
                } else {
                    band
                }
            }
            current.copy(equalizer = current.equalizer.copy(bands = updatedBands))
        }
    }

    override suspend fun setBassBoost(strength: Int) {
        _state.update { current ->
            current.copy(equalizer = current.equalizer.copy(bassBoostStrength = strength.coerceIn(0, 1000)))
        }
    }

    override fun updatePosition(positionMs: Long) {
        _state.update { current ->
            val clamped = positionMs.coerceIn(0L, current.durationMs.coerceAtLeast(0L))
            current.copy(
                currentPositionMs = clamped,
                progress = AudioPlayerMapper.calculateProgress(clamped, current.durationMs)
            )
        }
    }

    override fun reset() {
        shuffleOrder = emptyList()
        _state.value = AudioPlaybackState(
            equalizer = org.telegram.messenger.feature.audioplayer.domain.model.EqualizerState(
                bands = listOf(
                    EqualizerBand(0, 60, 0),
                    EqualizerBand(1, 230, 0),
                    EqualizerBand(2, 910, 0),
                    EqualizerBand(3, 3600, 0),
                    EqualizerBand(4, 14000, 0)
                )
            )
        )
    }

    private fun regenerateShuffleOrder(size: Int, currentTrackIndex: Int) {
        if (size <= 0) {
            shuffleOrder = emptyList()
            return
        }
        val remaining = (0 until size).filter { it != currentTrackIndex }.shuffled()
        shuffleOrder = if (currentTrackIndex in 0 until size) {
            listOf(currentTrackIndex) + remaining
        } else {
            (0 until size).shuffled()
        }
    }
}
