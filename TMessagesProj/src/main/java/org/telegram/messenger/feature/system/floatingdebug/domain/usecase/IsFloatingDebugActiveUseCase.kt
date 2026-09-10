package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class IsFloatingDebugActiveUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(): Boolean = repository.isActive()
}
