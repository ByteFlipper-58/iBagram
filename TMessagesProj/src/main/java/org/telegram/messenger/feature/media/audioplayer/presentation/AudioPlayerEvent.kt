package org.telegram.messenger.feature.media.audioplayer.presentation

import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioOutputRoute
import org.telegram.messenger.feature.media.audioplayer.domain.model.AudioTrackModel

sealed class AudioPlayerEvent {
    data class Play(
        val track: AudioTrackModel,
        val playlist: List<AudioTrackModel> = listOf(track)
    ) : AudioPlayerEvent()

    object TogglePlayPause : AudioPlayerEvent()
    object Stop : AudioPlayerEvent()
    data class SeekToPosition(val positionMs: Long) : AudioPlayerEvent()
    data class SeekToProgress(val progress: Float) : AudioPlayerEvent()
    object NextTrack : AudioPlayerEvent()
    object PreviousTrack : AudioPlayerEvent()
    object CycleSpeed : AudioPlayerEvent()
    data class SetSpeed(val speed: Float) : AudioPlayerEvent()
    object ToggleRepeatMode : AudioPlayerEvent()
    object ToggleShuffle : AudioPlayerEvent()
    data class SetProximity(val isNear: Boolean) : AudioPlayerEvent()
    data class SetOutputRoute(val route: AudioOutputRoute) : AudioPlayerEvent()
    data class ToggleEqualizer(val isEnabled: Boolean) : AudioPlayerEvent()
    data class SetBassBoost(val strength: Int) : AudioPlayerEvent()
}
