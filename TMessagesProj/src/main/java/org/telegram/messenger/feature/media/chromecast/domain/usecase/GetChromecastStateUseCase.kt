package org.telegram.messenger.feature.media.chromecast.domain.usecase

import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository

class GetChromecastStateUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(): ChromecastStateModel = repository.getChromecastState()
}
