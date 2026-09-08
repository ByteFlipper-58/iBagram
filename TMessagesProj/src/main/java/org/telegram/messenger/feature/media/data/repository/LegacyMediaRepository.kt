package org.telegram.messenger.feature.media.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.MediaController
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.data.mapper.MediaMapper
import org.telegram.messenger.feature.media.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository

/**
 * Adapter implementing [MediaRepository] on top of legacy [MediaController].
 * Ensures thread-safety by reading albums and photos on [Dispatchers.Main].
 */
class LegacyMediaRepository(
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : MediaRepository {

    private val _albumsFlow = MutableStateFlow<List<MediaAlbumModel>>(emptyList())
    val albumsFlow: Flow<List<MediaAlbumModel>> = _albumsFlow.asStateFlow()

    private fun loadAlbumsSync(): List<MediaAlbumModel> {
        val albums = MediaController.allMediaAlbums
        if (albums.isEmpty()) {
            MediaController.loadGalleryPhotosAlbums(0)
        }
        val mapped = albums.mapNotNull { MediaMapper.mapAlbumEntry(it) }
        _albumsFlow.value = mapped
        return mapped
    }

    override fun observeAlbums(): Flow<List<MediaAlbumModel>> {
        loadAlbumsSync()
        return albumsFlow
    }

    override suspend fun getAlbums(): Result<List<MediaAlbumModel>> = withContext(mainDispatcher) {
        try {
            val albums = loadAlbumsSync()
            Result.success(albums)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to get media albums", e))
        }
    }

    override suspend fun getMediaForAlbum(albumId: Int): Result<List<MediaItemModel>> = withContext(mainDispatcher) {
        try {
            val album = MediaController.allMediaAlbums.firstOrNull { it.bucketId == albumId }
                ?: MediaController.allPhotoAlbums.firstOrNull { it.bucketId == albumId }
            val items = album?.photos?.mapNotNull { MediaMapper.mapPhotoEntry(it) } ?: emptyList()
            Result.success(items)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to get media for album $albumId", e))
        }
    }

    override suspend fun getAllMedia(): Result<List<MediaItemModel>> = withContext(mainDispatcher) {
        try {
            val allPhotos = MediaController.allPhotosAlbumEntry
                ?: MediaController.allMediaAlbumEntry
            val items = allPhotos?.photos?.mapNotNull { MediaMapper.mapPhotoEntry(it) } ?: emptyList()
            Result.success(items)
        } catch (e: Throwable) {
            Result.failure(AppError.Generic(e.message ?: "Failed to get all media", e))
        }
    }
}
