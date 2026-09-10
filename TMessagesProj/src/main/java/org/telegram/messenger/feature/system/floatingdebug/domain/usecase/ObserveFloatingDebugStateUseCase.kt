package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class ObserveFloatingDebugStateUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(): Flow<FloatingDebugState> = repository.observeState()
}
