package org.telegram.messenger.feature.audioplayer.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.audioplayer.domain.model.AudioPlaybackState
import org.telegram.messenger.feature.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.audioplayer.domain.model.RepeatMode

interface AudioPlayerRepository {
    fun observePlaybackState(): Flow<AudioPlaybackState>
    fun getPlaybackState(): AudioPlaybackState
    suspend fun play(track: AudioTrackModel, playlist: List<AudioTrackModel>)
    suspend fun resume(): Boolean
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(positionMs: Long)
    suspend fun seekToProgress(progress: Float)
    suspend fun next(): Boolean
    suspend fun previous(): Boolean
    suspend fun setPlaybackSpeed(speed: Float)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun toggleRepeatMode(): RepeatMode
    suspend fun toggleShuffle(): Boolean
    suspend fun setOutputRoute(route: AudioOutputRoute)
    suspend fun setProximityNear(isNear: Boolean)
    suspend fun setEqualizerEnabled(enabled: Boolean)
    suspend fun setEqualizerBandGain(bandIndex: Int, gainMilliBels: Int)
    suspend fun setBassBoost(strength: Int)
    fun updatePosition(positionMs: Long)
    fun reset()
}
