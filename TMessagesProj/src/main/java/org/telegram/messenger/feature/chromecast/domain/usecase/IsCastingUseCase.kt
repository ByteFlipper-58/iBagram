package org.telegram.messenger.feature.chromecast.domain.usecase

import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository

class IsCastingUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(): Boolean = repository.isCasting()
}
