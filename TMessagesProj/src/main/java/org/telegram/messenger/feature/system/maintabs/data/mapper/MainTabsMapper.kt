package org.telegram.messenger.feature.system.maintabs.data.mapper

import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType

object MainTabsMapper {
    fun positionToTab(position: Int, showCallsTab: Boolean): MainTabType {
        return MainTabType.fromPosition(position, showCallsTab)
    }

    fun indexToTab(index: Int): MainTabType {
        return MainTabType.fromLegacyIndex(index)
    }

    fun tabToPosition(tab: MainTabType): Int {
        return tab.position
    }

    fun tabToLegacyIndex(tab: MainTabType): Int {
        return tab.legacyIndex
    }

    fun formatBadgeCount(count: Int): String? {
        return if (count > 0) count.toString() else null
    }
}
