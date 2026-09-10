package org.telegram.messenger.feature.media.chromecast.domain.usecase

import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository

class IsMediaPlayingOnCastUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(media: ChromecastMediaModel): Boolean = repository.isPlaying(media)
}
