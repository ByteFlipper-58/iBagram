package org.telegram.messenger.feature.media.mediadata.data.mapper

import org.telegram.messenger.MediaController
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel

/**
 * Pure mapper converting [MediaController.PhotoEntry] and [MediaController.AlbumEntry] into domain models.
 */
object MediaMapper {

    fun mapPhotoEntry(entry: MediaController.PhotoEntry?): MediaItemModel? {
        if (entry == null) return null
        val filePath = entry.path ?: entry.imagePath ?: ""
        return MediaItemModel(
            id = entry.imageId,
            bucketId = entry.bucketId,
            path = filePath,
            isVideo = entry.isVideo,
            duration = entry.duration,
            dateTaken = entry.dateTaken,
            width = entry.width,
            height = entry.height,
            size = entry.size,
            hasSpoiler = entry.hasSpoiler
        )
    }

    fun mapAlbumEntry(album: MediaController.AlbumEntry?): MediaAlbumModel? {
        if (album == null) return null
        val items = album.photos?.mapNotNull { mapPhotoEntry(it) } ?: emptyList()
        val coverPath = album.coverPhoto?.let { it.path ?: it.imagePath }
        return MediaAlbumModel(
            id = album.bucketId,
            name = album.bucketName ?: "",
            coverPath = coverPath,
            mediaCount = album.photos?.size ?: items.size,
            items = items
        )
    }
}
