package org.telegram.messenger.feature.system.refreshrate.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.refreshrate.data.datasource.RefreshRateLocalDataSource
import org.telegram.messenger.feature.system.refreshrate.data.datasource.RefreshRateRemoteDataSource
import org.telegram.messenger.feature.system.refreshrate.domain.model.DisplayRefreshModeModel
import org.telegram.messenger.feature.system.refreshrate.domain.model.RefreshRateStateModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository

class RefreshRateRepositoryImpl(
    val localDataSource: RefreshRateLocalDataSource,
    val remoteDataSource: RefreshRateRemoteDataSource,
    val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : RefreshRateRepository {

    override fun observeState(): Flow<RefreshRateStateModel> = localDataSource.state

    override fun getState(): RefreshRateStateModel = localDataSource.getState()

    override fun startTracking(): Result<Unit> {
        localDataSource.startTracking()
        return Result.Success(Unit)
    }

    override fun stopTracking(): Result<Unit> {
        localDataSource.stopTracking()
        return Result.Success(Unit)
    }

    override fun setAdaptiveEnabled(enabled: Boolean): Result<Unit> {
        localDataSource.setAdaptiveEnabled(enabled)
        return Result.Success(Unit)
    }

    override fun setPreferredMode(mode: DisplayRefreshModeModel): Result<Unit> {
        localDataSource.setPreferredMode(mode)
        return Result.Success(Unit)
    }

    override fun recordFrameDuration(durationNs: Long): Result<Unit> {
        localDataSource.recordFrameDuration(durationNs)
        return Result.Success(Unit)
    }

    override fun resetStats(): Result<Unit> {
        localDataSource.resetStats()
        return Result.Success(Unit)
    }

    override fun getAvailableModes(): List<DisplayRefreshModeModel> {
        return localDataSource.getAvailableModes()
    }
}
