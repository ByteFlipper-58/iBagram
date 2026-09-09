package org.telegram.messenger.feature.floatingdebug.domain.usecase

import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository

class ToggleFloatingDebugActiveUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(saveConfig: Boolean = true): Boolean {
        return repository.toggleActive(saveConfig)
    }
}
