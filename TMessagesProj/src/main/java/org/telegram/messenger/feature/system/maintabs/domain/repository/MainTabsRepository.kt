package org.telegram.messenger.feature.system.maintabs.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel

interface MainTabsRepository {
    fun observeConfig(): Flow<MainTabsConfigModel>
    fun getConfig(): MainTabsConfigModel
    fun setTabsVisible(visible: Boolean)
    fun selectTab(tab: MainTabType)
    fun selectPosition(position: Int)
    fun setShowCallsTab(show: Boolean)
    fun updateChatsUnreadCount(unreadCount: Int)
    fun setContactsPermissionWarning(hasWarning: Boolean)
}
