package org.telegram.messenger.feature.system.maintabs.presentation

import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel

data class MainTabsUiState(
    val config: MainTabsConfigModel = MainTabsConfigModel()
) {
    val isTabsVisible: Boolean
        get() = config.isTabsVisible

    val selectedTab: MainTabType
        get() = config.selectedTab

    val visibleTabs: List<MainTabType>
        get() = config.visibleTabs

    val showCallsTab: Boolean
        get() = config.showCallsTab

    val chatsUnreadCount: Int
        get() = config.chatsUnreadCount

    val hasContactsPermissionWarning: Boolean
        get() = config.hasContactsPermissionWarning
}
