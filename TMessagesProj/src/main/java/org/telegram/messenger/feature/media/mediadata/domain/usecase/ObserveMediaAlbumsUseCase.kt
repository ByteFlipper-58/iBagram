package org.telegram.messenger.feature.media.mediadata.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.mediadata.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.mediadata.domain.repository.MediaRepository

class ObserveMediaAlbumsUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaAlbumModel>> = repository.observeAlbums()
}
