package org.telegram.messenger.feature.media.pip.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.pip.data.datasource.PipLocalDataSource
import org.telegram.messenger.feature.media.pip.data.datasource.PipRemoteDataSource
import org.telegram.messenger.feature.media.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

/**
 * Modern implementation of [PipRepository] coordinating local session state
 * and remote/system actions.
 */
class PipRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: PipLocalDataSource,
    private val remoteDataSource: PipRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : PipRepository {

    override fun observeSessionInfo(): Flow<PipSessionInfo> =
        localDataSource.observeSessionInfo()

    override fun getSessionInfo(): PipSessionInfo =
        localDataSource.getSessionInfo()

    override fun registerSource(source: PipSourceModel) {
        localDataSource.registerSource(source)
    }

    override fun unregisterSource(tag: String) {
        localDataSource.unregisterSource(tag)
    }

    override fun updateSourceAvailability(tag: String, isAvailable: Boolean) {
        localDataSource.updateSourceAvailability(tag, isAvailable)
    }

    override fun updateSourceRatio(tag: String, width: Int, height: Int) {
        localDataSource.updateSourceRatio(tag, width, height)
    }

    override fun updateSourceAttached(tag: String, isAttached: Boolean) {
        localDataSource.updateSourceAttached(tag, isAttached)
    }

    override fun updatePipState(state: PipState, byActivityStop: Boolean) {
        localDataSource.updatePipState(state, byActivityStop)
    }

    override fun triggerPipAction(tag: String, actionId: Int) {
        localDataSource.recordPipAction(tag, actionId)
    }

    override fun canEnterPip(): Boolean =
        localDataSource.canEnterPip()
}
