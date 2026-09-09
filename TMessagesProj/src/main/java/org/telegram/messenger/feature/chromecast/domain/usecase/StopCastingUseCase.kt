package org.telegram.messenger.feature.chromecast.domain.usecase

import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository

class StopCastingUseCase(
    private val repository: ChromecastRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.stopCasting()
}
