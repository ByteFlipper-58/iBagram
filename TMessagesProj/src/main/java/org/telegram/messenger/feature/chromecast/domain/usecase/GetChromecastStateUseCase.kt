package org.telegram.messenger.feature.chromecast.domain.usecase

import org.telegram.messenger.feature.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository

class GetChromecastStateUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(): ChromecastStateModel = repository.getChromecastState()
}
