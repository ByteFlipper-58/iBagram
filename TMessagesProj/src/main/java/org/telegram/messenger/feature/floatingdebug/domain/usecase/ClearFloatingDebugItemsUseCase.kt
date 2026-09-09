package org.telegram.messenger.feature.floatingdebug.domain.usecase

import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository

class ClearFloatingDebugItemsUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke() {
        repository.clearDebugItems()
    }
}
