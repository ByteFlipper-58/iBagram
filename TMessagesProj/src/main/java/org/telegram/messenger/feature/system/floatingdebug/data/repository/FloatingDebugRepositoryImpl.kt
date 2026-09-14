package org.telegram.messenger.feature.system.floatingdebug.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.floatingdebug.data.datasource.FloatingDebugLocalDataSource
import org.telegram.messenger.feature.system.floatingdebug.data.datasource.FloatingDebugRemoteDataSource
import org.telegram.messenger.feature.system.floatingdebug.domain.model.DebugItemModel
import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository

/**
 * Implementation of [FloatingDebugRepository] coordinating clean local and remote data sources.
 */
class FloatingDebugRepositoryImpl(
    private val localDataSource: FloatingDebugLocalDataSource,
    private val remoteDataSource: FloatingDebugRemoteDataSource
) : FloatingDebugRepository {

    override fun isActive(): Boolean {
        return localDataSource.isActive()
    }

    override fun setActive(active: Boolean, saveConfig: Boolean) {
        localDataSource.setActive(active, saveConfig)
    }

    override fun toggleActive(saveConfig: Boolean): Boolean {
        return localDataSource.toggleActive(saveConfig)
    }

    override fun getDebugItems(): List<DebugItemModel> {
        return localDataSource.getDebugItems()
    }

    override fun registerDebugItems(items: List<DebugItemModel>) {
        localDataSource.registerDebugItems(items)
    }

    override fun clearDebugItems() {
        localDataSource.clearDebugItems()
    }

    override fun getState(): FloatingDebugState {
        return localDataSource.getState()
    }

    override fun observeState(): Flow<FloatingDebugState> {
        return localDataSource.observeState()
    }
}
