package org.telegram.messenger.feature.media.mediadata.domain.model

/**
 * Pure Kotlin immutable domain model representing a media album / gallery folder.
 * Decouples presentation and domain layers from mutable [org.telegram.messenger.MediaController.AlbumEntry].
 */
data class MediaAlbumModel(
    val id: Int,
    val name: String,
    val coverPath: String? = null,
    val mediaCount: Int = 0,
    val items: List<MediaItemModel> = emptyList()
)
