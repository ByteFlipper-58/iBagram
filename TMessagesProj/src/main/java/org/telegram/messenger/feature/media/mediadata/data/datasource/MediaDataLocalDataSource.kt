package org.telegram.messenger.feature.media.mediadata.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.MediaController
import org.telegram.messenger.feature.media.mediadata.data.mapper.MediaMapper
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel
import java.util.concurrent.ConcurrentHashMap

class MediaDataLocalDataSource(
    private val currentAccount: Int = 0
) {
    private val albumsMap = ConcurrentHashMap<Int, MediaAlbumModel>()
    private val _albumsFlow = MutableStateFlow<List<MediaAlbumModel>>(emptyList())
    val albumsFlow: StateFlow<List<MediaAlbumModel>> = _albumsFlow.asStateFlow()

    fun getAlbums(): List<MediaAlbumModel> {
        if (albumsMap.isNotEmpty()) {
            return albumsMap.values.toList()
        }

        try {
            val allAlbums = MediaController.allMediaAlbums
            if (allAlbums != null && allAlbums.isNotEmpty()) {
                val mapped = allAlbums.mapNotNull { MediaMapper.mapAlbumEntry(it) }
                setAlbums(mapped)
                return mapped
            }
        } catch (_: Throwable) {
            // JVM test fallback
        }

        return _albumsFlow.value
    }

    fun getMediaForAlbum(albumId: Int): List<MediaItemModel> {
        val album = albumsMap[albumId]
        if (album != null) {
            return album.items
        }

        try {
            val allAlbums = MediaController.allMediaAlbums
            val found = allAlbums?.firstOrNull { it.bucketId == albumId }
            if (found != null) {
                val mapped = MediaMapper.mapAlbumEntry(found)
                if (mapped != null) {
                    addAlbum(mapped)
                    return mapped.items
                }
            }
        } catch (_: Throwable) {
            // JVM test fallback
        }

        return emptyList()
    }

    fun getAllMedia(): List<MediaItemModel> {
        val albums = getAlbums()
        return albums.flatMap { it.items }
    }

    fun setAlbums(albums: List<MediaAlbumModel>) {
        albumsMap.clear()
        for (album in albums) {
            albumsMap[album.id] = album
        }
        _albumsFlow.value = albumsMap.values.toList()
    }

    fun addAlbum(album: MediaAlbumModel) {
        albumsMap[album.id] = album
        _albumsFlow.value = albumsMap.values.toList()
    }

    fun clearAlbums() {
        albumsMap.clear()
        _albumsFlow.value = emptyList()
    }
}
