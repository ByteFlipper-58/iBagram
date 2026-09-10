package org.telegram.messenger.feature.system.maintabs.presentation

import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType

sealed interface MainTabsEvent {
    data class SetTabsVisible(val visible: Boolean) : MainTabsEvent
    data class SelectTab(val tab: MainTabType) : MainTabsEvent
    data class SelectPosition(val position: Int) : MainTabsEvent
    data class SetShowCallsTab(val show: Boolean) : MainTabsEvent
    data class UpdateUnreadCount(val count: Int) : MainTabsEvent
    data class SetContactsWarning(val warning: Boolean) : MainTabsEvent
}
