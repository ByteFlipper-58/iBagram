package org.telegram.messenger.feature.media.mediadata.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.mediadata.data.datasource.MediaDataLocalDataSource
import org.telegram.messenger.feature.media.mediadata.data.datasource.MediaDataRemoteDataSource
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.mediadata.domain.repository.MediaRepository

class MediaDataRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: MediaDataLocalDataSource,
    private val remoteDataSource: MediaDataRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    override fun observeAlbums(): Flow<List<MediaAlbumModel>> {
        return localDataSource.albumsFlow
    }

    override suspend fun getAlbums(): Result<List<MediaAlbumModel>> = withContext(dispatcher) {
        try {
            val albums = localDataSource.getAlbums()
            Result.Success(albums)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get albums", e))
        }
    }

    override suspend fun getMediaForAlbum(albumId: Int): Result<List<MediaItemModel>> = withContext(dispatcher) {
        try {
            remoteDataSource.syncRemoteMedia(albumId)
            val media = localDataSource.getMediaForAlbum(albumId)
            Result.Success(media)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get media for album", e))
        }
    }

    override suspend fun getAllMedia(): Result<List<MediaItemModel>> = withContext(dispatcher) {
        try {
            val media = localDataSource.getAllMedia()
            Result.Success(media)
        } catch (e: Exception) {
            Result.Failure(AppError.Generic(e.message ?: "Failed to get all media", e))
        }
    }
}
