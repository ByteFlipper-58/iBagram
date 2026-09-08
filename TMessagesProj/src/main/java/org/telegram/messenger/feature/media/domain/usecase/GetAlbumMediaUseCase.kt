package org.telegram.messenger.feature.media.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository

class GetAlbumMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(albumId: Int): Result<List<MediaItemModel>> = repository.getMediaForAlbum(albumId)
}
