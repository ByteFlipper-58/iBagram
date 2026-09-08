package org.telegram.messenger.feature.media.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.domain.model.MediaAlbumModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository

class ObserveMediaAlbumsUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaAlbumModel>> = repository.observeAlbums()
}
