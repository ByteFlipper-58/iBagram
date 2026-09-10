package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class GetFloatingDebugStateUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(): FloatingDebugState = repository.getState()
}
