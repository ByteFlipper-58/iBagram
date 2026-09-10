package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class ClearFloatingDebugItemsUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke() {
        repository.clearDebugItems()
    }
}
