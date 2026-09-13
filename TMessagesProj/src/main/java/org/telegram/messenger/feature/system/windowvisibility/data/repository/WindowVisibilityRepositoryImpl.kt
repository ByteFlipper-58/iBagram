package org.telegram.messenger.feature.system.windowvisibility.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.windowvisibility.data.datasource.WindowVisibilityLocalDataSource
import org.telegram.messenger.feature.system.windowvisibility.data.datasource.WindowVisibilityRemoteDataSource
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityController
import org.telegram.messenger.feature.system.windowvisibility.domain.model.WindowVisibilityState
import org.telegram.messenger.feature.system.windowvisibility.domain.repository.WindowVisibilityRepository

/**
 * Production implementation of [WindowVisibilityRepository] coordinating local arbitration and remote policies.
 */
class WindowVisibilityRepositoryImpl(
    private val localDataSource: WindowVisibilityLocalDataSource,
    private val remoteDataSource: WindowVisibilityRemoteDataSource
) : WindowVisibilityRepository {

    override fun requestHide(reasonTag: String, description: String): WindowVisibilityState {
        return localDataSource.requestHide(reasonTag, description)
    }

    override fun releaseHide(reasonTag: String): WindowVisibilityState {
        return localDataSource.releaseHide(reasonTag)
    }

    override fun toggleHide(reasonTag: String, hide: Boolean, description: String): WindowVisibilityState {
        return localDataSource.toggleHide(reasonTag, hide, description)
    }

    override fun isVisible(): Boolean {
        return localDataSource.isVisible()
    }

    override fun isHidden(): Boolean {
        return localDataSource.isHidden()
    }

    override fun getReasonsCount(): Int {
        return localDataSource.getReasonsCount()
    }

    override fun getActiveReasons(): Set<String> {
        return localDataSource.getActiveReasons()
    }

    override fun getCurrentState(): WindowVisibilityState {
        return localDataSource.getCurrentState()
    }

    override fun resetAllReasons(): WindowVisibilityState {
        return localDataSource.resetAllReasons()
    }

    override fun observeState(): StateFlow<WindowVisibilityState> {
        return localDataSource.stateFlow
    }

    override fun observeVisibilityChanges(): Flow<Boolean> {
        return localDataSource.visibilityFlow
    }

    override fun obtainController(reasonTag: String): WindowVisibilityController {
        return localDataSource.obtainController(reasonTag)
    }
}
