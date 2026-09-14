package org.telegram.messenger.feature.system.maintabs.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.maintabs.data.datasource.MainTabsLocalDataSource
import org.telegram.messenger.feature.system.maintabs.data.datasource.MainTabsRemoteDataSource
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabType
import org.telegram.messenger.feature.system.maintabs.domain.model.MainTabsConfigModel
import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository

class MainTabsRepositoryImpl(
    private val localDataSource: MainTabsLocalDataSource,
    private val remoteDataSource: MainTabsRemoteDataSource
) : MainTabsRepository {

    override fun observeConfig(): Flow<MainTabsConfigModel> = localDataSource.observeConfig()

    override fun getConfig(): MainTabsConfigModel = localDataSource.getConfig()

    override fun setTabsVisible(visible: Boolean) {
        localDataSource.setTabsVisible(visible)
    }

    override fun selectTab(tab: MainTabType) {
        localDataSource.selectTab(tab)
    }

    override fun selectPosition(position: Int) {
        localDataSource.selectPosition(position)
    }

    override fun setShowCallsTab(show: Boolean) {
        localDataSource.setShowCallsTab(show)
    }

    override fun updateChatsUnreadCount(unreadCount: Int) {
        localDataSource.updateChatsUnreadCount(unreadCount)
    }

    override fun setContactsPermissionWarning(hasWarning: Boolean) {
        localDataSource.setContactsPermissionWarning(hasWarning)
    }
}
