package org.telegram.messenger.feature.audioplayer.data.mapper

import org.telegram.messenger.feature.audioplayer.domain.model.RepeatMode

object AudioPlayerMapper {

    fun formatTimeMs(millis: Long): String {
        if (millis <= 0L) return "00:00"
        val totalSeconds = millis / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun calculateProgress(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L || positionMs <= 0L) return 0f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun calculatePosition(progress: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val clampedProgress = progress.coerceIn(0f, 1f)
        return (clampedProgress * durationMs).toLong()
    }

    fun resolveNextTrackIndex(
        currentIndex: Int,
        playlistSize: Int,
        repeatMode: RepeatMode,
        isShuffle: Boolean = false,
        shuffleIndices: List<Int>? = null
    ): Int? {
        if (playlistSize <= 0) return null
        if (repeatMode == RepeatMode.CURRENT) {
            return currentIndex.coerceIn(0, playlistSize - 1)
        }

        if (isShuffle && !shuffleIndices.isNullOrEmpty()) {
            val shufflePos = shuffleIndices.indexOf(currentIndex)
            if (shufflePos != -1) {
                val nextShufflePos = shufflePos + 1
                return if (nextShufflePos < shuffleIndices.size) {
                    shuffleIndices[nextShufflePos]
                } else if (repeatMode == RepeatMode.ALL) {
                    shuffleIndices.firstOrNull()
                } else {
                    null
                }
            }
        }

        val nextIndex = currentIndex + 1
        return if (nextIndex < playlistSize) {
            nextIndex
        } else if (repeatMode == RepeatMode.ALL) {
            0
        } else {
            null
        }
    }

    fun resolvePreviousTrackIndex(
        currentIndex: Int,
        playlistSize: Int,
        repeatMode: RepeatMode,
        isShuffle: Boolean = false,
        shuffleIndices: List<Int>? = null
    ): Int? {
        if (playlistSize <= 0) return null
        if (repeatMode == RepeatMode.CURRENT) {
            return currentIndex.coerceIn(0, playlistSize - 1)
        }

        if (isShuffle && !shuffleIndices.isNullOrEmpty()) {
            val shufflePos = shuffleIndices.indexOf(currentIndex)
            if (shufflePos != -1) {
                val prevShufflePos = shufflePos - 1
                return if (prevShufflePos >= 0) {
                    shuffleIndices[prevShufflePos]
                } else if (repeatMode == RepeatMode.ALL) {
                    shuffleIndices.lastOrNull()
                } else {
                    shuffleIndices.firstOrNull()
                }
            }
        }

        val prevIndex = currentIndex - 1
        return if (prevIndex >= 0) {
            prevIndex
        } else if (repeatMode == RepeatMode.ALL) {
            playlistSize - 1
        } else {
            0
        }
    }

    fun clampSpeed(speed: Float): Float {
        val clamped = speed.coerceIn(0.5f, 2.5f)
        return Math.round(clamped * 10f) / 10f
    }
}
