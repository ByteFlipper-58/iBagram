package org.telegram.messenger.feature.media.mediadata.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.mediadata.domain.repository.MediaRepository

class GetAlbumMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(albumId: Int): Result<List<MediaItemModel>> = repository.getMediaForAlbum(albumId)
}
