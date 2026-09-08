package org.telegram.messenger.feature.media.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository

class GetMediaAlbumsUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Result<List<MediaAlbumModel>> = repository.getAlbums()
}
