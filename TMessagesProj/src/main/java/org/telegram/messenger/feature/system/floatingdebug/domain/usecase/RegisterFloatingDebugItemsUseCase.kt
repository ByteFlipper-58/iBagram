package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class RegisterFloatingDebugItemsUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(items: List<DebugItemModel>) {
        repository.registerDebugItems(items)
    }
}
