package org.telegram.messenger.feature.media.photoviewer.domain.model

/**
 * Supported media types for fullscreen photo/video viewer.
 */
enum class ViewerMediaType {
    PHOTO,
    VIDEO,
    GIF,
    ANIMATED_STICKER,
    ROUND_VIDEO
}

/**
 * Selection modes matching legacy PhotoViewer SELECT_TYPE_* constants.
 */
enum class ViewerSelectType(val legacyId: Int) {
    NO_SELECT(-1),
    AVATAR(1),
    WALLPAPER(3),
    QR(10),
    STICKER(11),
    GIF(12),
    POLL_MEDIA(13),
    POLL_MEDIA_EDIT(14);

    companion object {
        fun fromLegacyId(id: Int): ViewerSelectType {
            return values().firstOrNull { it.legacyId == id } ?: NO_SELECT
        }
    }
}

/**
 * Photo/video editing modes matching legacy PhotoViewer EDIT_MODE_* constants.
 */
enum class ViewerEditMode(val legacyId: Int) {
    NONE(0),
    CROP(1),
    FILTER(2),
    PAINT(3),
    STICKER_MASK(4),
    COVER(5);

    companion object {
        fun fromLegacyId(id: Int): ViewerEditMode {
            return values().firstOrNull { it.legacyId == id } ?: NONE
        }
    }
}

/**
 * Actions that can be performed on the current media item.
 */
enum class ViewerActionType {
    SEND,
    FORWARD,
    SHARE,
    SAVE_TO_GALLERY,
    DELETE,
    EDIT,
    SET_AVATAR,
    ROTATE,
    PIP,
    SPEED,
    QUALITY
}

/**
 * Immutable domain model representing a single media item in the viewer.
 */
data class PhotoViewerMediaItem(
    val id: Long,
    val messageId: Long = 0L,
    val dialogId: Long = 0L,
    val mediaType: ViewerMediaType = ViewerMediaType.PHOTO,
    val path: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationSeconds: Int = 0,
    val sizeBytes: Long = 0L,
    val caption: String? = null,
    val isCaptionAbove: Boolean = false,
    val hasSpoiler: Boolean = false,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false
)

/**
 * Playback state for video/animated media.
 */
data class PhotoViewerPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val quality: Int = 720,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false,
    val isBuffering: Boolean = false
) {
    val progress: Float
        get() = if (durationMs > 0L) (currentPositionMs.toFloat() / durationMs).coerceIn(0.0f, 1.0f) else 0.0f
}

/**
 * Pan, zoom, and rotation transform state for viewer gestures.
 */
data class PhotoViewerTransform(
    val scale: Float = 1.0f,
    val translationX: Float = 0.0f,
    val translationY: Float = 0.0f,
    val rotation: Float = 0.0f,
    val minScale: Float = 1.0f,
    val maxScale: Float = 3.0f
)

/**
 * Overall domain state for the PhotoViewer session.
 */
data class PhotoViewerState(
    val items: List<PhotoViewerMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val selectType: ViewerSelectType = ViewerSelectType.NO_SELECT,
    val editMode: ViewerEditMode = ViewerEditMode.NONE,
    val isVisible: Boolean = false,
    val playbackState: PhotoViewerPlaybackState = PhotoViewerPlaybackState(),
    val transform: PhotoViewerTransform = PhotoViewerTransform(),
    val isActionBarVisible: Boolean = true,
    val isCaptionExpanded: Boolean = false,
    val availableActions: Set<ViewerActionType> = emptySet()
) {
    val currentItem: PhotoViewerMediaItem?
        get() = items.getOrNull(currentIndex)

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasNext: Boolean
        get() = currentIndex < items.size - 1

    val totalCount: Int
        get() = items.size
}
