package org.telegram.messenger.feature.system.appconfig.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.appconfig.data.datasource.AppConfigLocalDataSource
import org.telegram.messenger.feature.system.appconfig.data.datasource.AppConfigRemoteDataSource
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState
import org.telegram.messenger.feature.system.appconfig.domain.repository.AppConfigRepository

/**
 * Implementation of [AppConfigRepository] coordinating local app configuration caching
 * and remote configuration policies.
 */
class AppConfigRepositoryImpl(
    private val localDataSource: AppConfigLocalDataSource,
    private val remoteDataSource: AppConfigRemoteDataSource
) : AppConfigRepository {

    override fun observeConfig(): Flow<AppGlobalConfigState> {
        return localDataSource.observeConfig()
    }

    override fun getConfig(): AppGlobalConfigState {
        return localDataSource.getConfig()
    }

    override suspend fun reloadConfig(): Result<AppGlobalConfigState> {
        return localDataSource.reloadConfig()
    }

    override suspend fun updateConfigValue(key: String, value: Any): Result<Unit> {
        return localDataSource.updateConfigValue(key, value)
    }
}
