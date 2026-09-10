package org.telegram.messenger.feature.audioplayer.domain.model

enum class AudioTrackType {
    VOICE,
    ROUND_VIDEO,
    MUSIC,
    PODCAST
}

enum class AudioPlaybackStatus {
    IDLE,
    PREPARING,
    PLAYING,
    PAUSED,
    SEEKING,
    ERROR
}

enum class RepeatMode {
    NONE,
    ALL,
    CURRENT
}

enum class AudioOutputRoute {
    SPEAKER,
    EARPIECE,
    HEADPHONES,
    BLUETOOTH
}

data class AudioTrackModel(
    val id: Long,
    val dialogId: Long,
    val messageId: Int,
    val title: String,
    val performer: String,
    val durationMs: Long,
    val fileSize: Long = 0L,
    val type: AudioTrackType = AudioTrackType.MUSIC,
    val artworkUrl: String? = null,
    val waveform: ByteArray? = null,
    val isRoundVideo: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTrackModel
        if (id != other.id) return false
        if (dialogId != other.dialogId) return false
        if (messageId != other.messageId) return false
        if (title != other.title) return false
        if (performer != other.performer) return false
        if (durationMs != other.durationMs) return false
        if (fileSize != other.fileSize) return false
        if (type != other.type) return false
        if (artworkUrl != other.artworkUrl) return false
        if (waveform != null) {
            if (other.waveform == null) return false
            if (!waveform.contentEquals(other.waveform)) return false
        } else if (other.waveform != null) return false
        if (isRoundVideo != other.isRoundVideo) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dialogId.hashCode()
        result = 31 * result + messageId
        result = 31 * result + title.hashCode()
        result = 31 * result + performer.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (artworkUrl?.hashCode() ?: 0)
        result = 31 * result + (waveform?.contentHashCode() ?: 0)
        result = 31 * result + isRoundVideo.hashCode()
        return result
    }
}

data class EqualizerBand(
    val bandIndex: Int,
    val centerFreqHz: Int,
    val gainMilliBels: Int,
    val minGainMilliBels: Int = -1500,
    val maxGainMilliBels: Int = 1500
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val presetName: String? = null,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val bands: List<EqualizerBand> = emptyList()
)

data class AudioPlaybackState(
    val currentTrack: AudioTrackModel? = null,
    val status: AudioPlaybackStatus = AudioPlaybackStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val isShuffleEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val outputRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val isProximityNear: Boolean = false,
    val playlist: List<AudioTrackModel> = emptyList(),
    val currentIndex: Int = -1,
    val equalizer: EqualizerState = EqualizerState(),
    val errorMessage: String? = null
) {
    val isPlaying: Boolean
        get() = status == AudioPlaybackStatus.PLAYING

    val isPaused: Boolean
        get() = status == AudioPlaybackStatus.PAUSED

    val isVoiceOrRoundVideo: Boolean
        get() = currentTrack?.type == AudioTrackType.VOICE || currentTrack?.type == AudioTrackType.ROUND_VIDEO

    val hasNext: Boolean
        get() = when (repeatMode) {
            RepeatMode.CURRENT -> currentTrack != null
            RepeatMode.ALL -> playlist.isNotEmpty()
            RepeatMode.NONE -> currentIndex in 0 until (playlist.size - 1)
        }

    val hasPrevious: Boolean
        get() = when (repeatMode) {
            RepeatMode.CURRENT -> currentTrack != null
            RepeatMode.ALL -> playlist.isNotEmpty()
            RepeatMode.NONE -> currentIndex > 0 || currentPositionMs > 3000L
        }
}
