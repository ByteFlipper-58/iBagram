package org.telegram.messenger.feature.system.litemode.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.feature.system.litemode.data.datasource.LiteModeLocalDataSource
import org.telegram.messenger.feature.system.litemode.data.datasource.LiteModeRemoteDataSource
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeFlag
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModePreset
import org.telegram.messenger.feature.system.litemode.domain.model.LiteModeState
import org.telegram.messenger.feature.system.litemode.domain.repository.LiteModeRepository

/**
 * Production implementation of [LiteModeRepository] coordinating local state and remote preset sync.
 */
class LiteModeRepositoryImpl(
    private val localDataSource: LiteModeLocalDataSource,
    private val remoteDataSource: LiteModeRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : LiteModeRepository {

    override fun observeLiteModeState(): Flow<LiteModeState> {
        return localDataSource.observeLiteModeState()
    }

    override fun getLiteModeState(): LiteModeState {
        return localDataSource.getLiteModeState()
    }

    override suspend fun setFlagEnabled(flag: LiteModeFlag, enabled: Boolean) = withContext(mainDispatcher) {
        localDataSource.setFlagEnabled(flag, enabled)
    }

    override suspend fun setAllFlags(rawFlags: Int) = withContext(mainDispatcher) {
        localDataSource.setAllFlags(rawFlags)
    }

    override suspend fun applyPreset(preset: LiteModePreset) = withContext(mainDispatcher) {
        localDataSource.applyPreset(preset)
    }

    override suspend fun setPowerSaverThreshold(percentage: Int) = withContext(mainDispatcher) {
        localDataSource.setPowerSaverThreshold(percentage)
    }

    override suspend fun updateBatteryLevel(level: Int) = withContext(mainDispatcher) {
        localDataSource.updateBatteryLevel(level)
    }

    override suspend fun setHasPremium(hasPremium: Boolean) = withContext(mainDispatcher) {
        localDataSource.setHasPremium(hasPremium)
    }

    override suspend fun reload() = withContext(mainDispatcher) {
        localDataSource.reload()
    }
}
