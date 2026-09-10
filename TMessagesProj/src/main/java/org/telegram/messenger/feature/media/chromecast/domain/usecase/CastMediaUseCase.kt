package org.telegram.messenger.feature.media.chromecast.domain.usecase

import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository

class CastMediaUseCase(
    private val repository: ChromecastRepository
) {
    suspend operator fun invoke(media: ChromecastMediaModel): Result<Unit> =
        repository.castMedia(media)
}
