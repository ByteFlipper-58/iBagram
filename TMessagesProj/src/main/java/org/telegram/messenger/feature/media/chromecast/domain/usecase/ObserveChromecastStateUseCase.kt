package org.telegram.messenger.feature.media.chromecast.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository

class ObserveChromecastStateUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(): Flow<ChromecastStateModel> = repository.observeChromecastState()
}
