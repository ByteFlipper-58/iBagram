package org.telegram.messenger.feature.media.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.domain.model.MediaItemModel
import org.telegram.messenger.feature.media.domain.repository.MediaRepository

class GetAllMediaUseCase(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Result<List<MediaItemModel>> = repository.getAllMedia()
}
