package org.telegram.messenger.feature.media.chromecast.domain.usecase

import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository

class StopCastingUseCase(
    private val repository: ChromecastRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.stopCasting()
}
