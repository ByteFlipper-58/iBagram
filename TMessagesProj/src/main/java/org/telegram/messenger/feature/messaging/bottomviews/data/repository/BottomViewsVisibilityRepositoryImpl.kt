package org.telegram.messenger.feature.messaging.bottomviews.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.bottomviews.data.datasource.BottomViewsLocalDataSource
import org.telegram.messenger.feature.messaging.bottomviews.data.datasource.BottomViewsRemoteDataSource
import org.telegram.messenger.feature.messaging.bottomviews.domain.model.BottomViewsVisibilityState
import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository

class BottomViewsVisibilityRepositoryImpl(
    private val currentAccount: Int = 0,
    private val localDataSource: BottomViewsLocalDataSource,
    private val remoteDataSource: BottomViewsRemoteDataSource
) : BottomViewsVisibilityRepository {

    override fun getVisibility(containerId: Int): Float {
        return localDataSource.getVisibility(containerId)
    }

    override fun setViewVisible(containerId: Int, isVisible: Boolean, animated: Boolean) {
        localDataSource.setViewVisible(containerId, isVisible, animated)
    }

    override fun getCurrentPriorityContainerId(): Int {
        return localDataSource.getCurrentPriorityContainerId()
    }

    override fun getState(): BottomViewsVisibilityState {
        return localDataSource.getState()
    }

    override fun observeState(): Flow<BottomViewsVisibilityState> {
        return localDataSource.state
    }
}
