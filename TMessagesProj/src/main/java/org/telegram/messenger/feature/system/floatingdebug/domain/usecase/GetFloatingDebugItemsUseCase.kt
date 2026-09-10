package org.telegram.messenger.feature.system.floatingdebug.domain.usecase

import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

class GetFloatingDebugItemsUseCase(
    private val repository: FloatingDebugRepository
) {
    operator fun invoke(): List<DebugItemModel> = repository.getDebugItems()
}
