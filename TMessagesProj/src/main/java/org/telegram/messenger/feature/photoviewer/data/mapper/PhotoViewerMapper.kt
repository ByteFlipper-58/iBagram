package org.telegram.messenger.feature.photoviewer.data.mapper

import org.telegram.messenger.feature.photoviewer.domain.model.ViewerEditMode
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerMediaType
import org.telegram.messenger.feature.photoviewer.domain.model.ViewerSelectType
import java.util.Locale

/**
 * Mapper for PhotoViewer legacy constants, media types, and playback time formatting.
 */
object PhotoViewerMapper {

    fun mapLegacySelectType(legacySelectType: Int): ViewerSelectType {
        return ViewerSelectType.fromLegacyId(legacySelectType)
    }

    fun toLegacySelectType(selectType: ViewerSelectType): Int {
        return selectType.legacyId
    }

    fun mapLegacyEditMode(legacyEditMode: Int): ViewerEditMode {
        return ViewerEditMode.fromLegacyId(legacyEditMode)
    }

    fun toLegacyEditMode(editMode: ViewerEditMode): Int {
        return editMode.legacyId
    }

    fun formatDurationSeconds(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
        }
    }

    fun formatPlaybackTimeMs(timeMs: Long): String {
        val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
        return formatDurationSeconds(totalSeconds)
    }

    fun getMediaTypeLabel(mediaType: ViewerMediaType): String {
        return when (mediaType) {
            ViewerMediaType.PHOTO -> "Photo"
            ViewerMediaType.VIDEO -> "Video"
            ViewerMediaType.GIF -> "GIF"
            ViewerMediaType.ANIMATED_STICKER -> "Sticker"
            ViewerMediaType.ROUND_VIDEO -> "Video Message"
        }
    }
}
