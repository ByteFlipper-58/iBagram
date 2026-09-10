package org.telegram.messenger.feature.system.floatingdebug.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState

/**
 * Contract for managing the floating debug tools overlay, visibility state and menu items.
 */
interface FloatingDebugRepository {
    fun isActive(): Boolean
    fun setActive(active: Boolean, saveConfig: Boolean = true)
    fun toggleActive(saveConfig: Boolean = true): Boolean
    fun getDebugItems(): List<DebugItemModel>
    fun registerDebugItems(items: List<DebugItemModel>)
    fun clearDebugItems()
    fun getState(): FloatingDebugState
    fun observeState(): Flow<FloatingDebugState>
}
