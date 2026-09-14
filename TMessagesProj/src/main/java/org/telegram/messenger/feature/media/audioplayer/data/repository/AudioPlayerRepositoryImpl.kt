package org.telegram.messenger.feature.media.audioplayer.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.audioplayer.data.datasource.AudioPlayerLocalDataSource
import org.telegram.messenger.feature.media.audioplayer.data.datasource.AudioPlayerRemoteDataSource
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioPlaybackState
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioTrackModel
import org.telegram.messenger.feature.media.audioplayer.domain.model.RepeatMode
import org.telegram.messenger.feature.media.audioplayer.domain.repository.AudioPlayerRepository

/**
 * Clean repository coordinating audio player state, queue manipulation,
 * and remote streaming configuration.
 */
class AudioPlayerRepositoryImpl(
    private val localDataSource: AudioPlayerLocalDataSource,
    private val remoteDataSource: AudioPlayerRemoteDataSource
) : AudioPlayerRepository {

    override fun observePlaybackState(): Flow<AudioPlaybackState> {
        return localDataSource.observePlaybackState()
    }

    override fun getPlaybackState(): AudioPlaybackState {
        return localDataSource.getPlaybackState()
    }

    override suspend fun play(track: AudioTrackModel, playlist: List<AudioTrackModel>) {
        localDataSource.play(track, playlist)
        remoteDataSource.reportAudioPlaybackEvent(track.dialogId, track.messageId)
    }

    override suspend fun resume(): Boolean {
        return localDataSource.resume()
    }

    override suspend fun pause() {
        localDataSource.pause()
    }

    override suspend fun stop() {
        localDataSource.stop()
    }

    override suspend fun seekTo(positionMs: Long) {
        localDataSource.seekTo(positionMs)
    }

    override suspend fun seekToProgress(progress: Float) {
        localDataSource.seekToProgress(progress)
    }

    override suspend fun next(): Boolean {
        return localDataSource.next()
    }

    override suspend fun previous(): Boolean {
        return localDataSource.previous()
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        localDataSource.setPlaybackSpeed(speed)
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        localDataSource.setRepeatMode(mode)
    }

    override suspend fun toggleRepeatMode(): RepeatMode {
        return localDataSource.toggleRepeatMode()
    }

    override suspend fun toggleShuffle(): Boolean {
        return localDataSource.toggleShuffle()
    }

    override suspend fun setOutputRoute(route: AudioOutputRoute) {
        localDataSource.setOutputRoute(route)
    }

    override suspend fun setProximityNear(isNear: Boolean) {
        localDataSource.setProximityNear(isNear)
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        localDataSource.setEqualizerEnabled(enabled)
    }

    override suspend fun setEqualizerBandGain(bandIndex: Int, gainMilliBels: Int) {
        localDataSource.setEqualizerBandGain(bandIndex, gainMilliBels)
    }

    override suspend fun setBassBoost(strength: Int) {
        localDataSource.setBassBoost(strength)
    }

    override fun updatePosition(positionMs: Long) {
        localDataSource.updatePosition(positionMs)
    }

    override fun reset() {
        localDataSource.reset()
    }
}
