package org.telegram.messenger.feature.chromecast.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository

class ObserveChromecastStateUseCase(
    private val repository: ChromecastRepository
) {
    operator fun invoke(): Flow<ChromecastStateModel> = repository.observeChromecastState()
}
