package org.telegram.messenger.feature.media.mediadata.domain.model

/**
 * Pure Kotlin immutable domain model representing a media item (photo or video).
 * Decouples presentation and domain layers from mutable [org.telegram.messenger.MediaController.PhotoEntry].
 */
data class MediaItemModel(
    val id: Int,
    val bucketId: Int,
    val path: String,
    val isVideo: Boolean = false,
    val duration: Int = 0,
    val dateTaken: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L,
    val hasSpoiler: Boolean = false
)
