package org.telegram.messenger.feature.floatingdebug.domain.usecase

import org.telegram.messenger.feature.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository

class RegisterFloatingDebugItemsUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(items: List<DebugItemModel>) {
        repository.registerDebugItems(items)
    }
}
