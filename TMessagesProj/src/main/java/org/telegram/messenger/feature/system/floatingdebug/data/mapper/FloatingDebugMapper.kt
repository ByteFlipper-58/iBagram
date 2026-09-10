package org.telegram.messenger.feature.system.floatingdebug.data.mapper

import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemKind
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.ui.Components.FloatingDebug.FloatingDebugController

object FloatingDebugMapper {

    fun toDomainKind(type: FloatingDebugController.DebugItemType): DebugItemKind {
        return when (type) {
            FloatingDebugController.DebugItemType.SIMPLE -> DebugItemKind.SIMPLE
            FloatingDebugController.DebugItemType.HEADER -> DebugItemKind.HEADER
            FloatingDebugController.DebugItemType.SEEKBAR -> DebugItemKind.SEEKBAR
        }
    }

    fun toState(isActive: Boolean, items: List<DebugItemModel>): FloatingDebugState {
        return FloatingDebugState(
            isActive = isActive,
            items = items
        )
    }
}
