package org.telegram.messenger.feature.media.mediadata.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel

/**
 * Clean domain repository contract for gallery media and album queries.
 */
interface MediaRepository {
    fun observeAlbums(): Flow<List<MediaAlbumModel>>
    suspend fun getAlbums(): Result<List<MediaAlbumModel>>
    suspend fun getMediaForAlbum(albumId: Int): Result<List<MediaItemModel>>
    suspend fun getAllMedia(): Result<List<MediaItemModel>>
}
