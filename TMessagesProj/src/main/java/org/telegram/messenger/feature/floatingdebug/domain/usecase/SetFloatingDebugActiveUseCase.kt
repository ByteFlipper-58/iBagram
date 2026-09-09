package org.telegram.messenger.feature.floatingdebug.domain.usecase

import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository

class SetFloatingDebugActiveUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(active: Boolean, saveConfig: Boolean = true) {
        repository.setActive(active, saveConfig)
    }
}
