package org.telegram.messenger.feature.messaging.factcheck.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository

class ObserveFactCheckLoadedUseCase(
    private val repository: FactCheckRepository
) {
    operator fun invoke(): Flow<Unit> = repository.observeFactCheckLoaded()
}
