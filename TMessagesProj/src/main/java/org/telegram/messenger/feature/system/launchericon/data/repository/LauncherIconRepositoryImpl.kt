package org.telegram.messenger.feature.system.launchericon.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.system.launchericon.data.datasource.LauncherIconLocalDataSource
import org.telegram.messenger.feature.system.launchericon.data.datasource.LauncherIconRemoteDataSource
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconModel
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconType
import org.telegram.messenger.feature.system.launchericon.domain.model.LauncherIconsStateModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository

/**
 * Production implementation of [LauncherIconRepository] coordinating local component management and remote sync.
 */
class LauncherIconRepositoryImpl(
    private val localDataSource: LauncherIconLocalDataSource,
    private val remoteDataSource: LauncherIconRemoteDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : LauncherIconRepository {

    override fun observeLauncherIcons(): Flow<LauncherIconsStateModel> {
        return localDataSource.observeLauncherIcons()
    }

    override fun getLauncherIcons(): List<LauncherIconModel> {
        return localDataSource.getLauncherIcons()
    }

    override fun getActiveIcon(): LauncherIconModel? {
        return localDataSource.getActiveIcon()
    }

    override fun isIconEnabled(type: LauncherIconType): Boolean {
        return localDataSource.isIconEnabled(type)
    }

    override suspend fun setIcon(type: LauncherIconType): Result<Boolean> = withContext(mainDispatcher) {
        val success = localDataSource.setIcon(type)
        if (success) {
            Result.Success(true)
        } else {
            Result.failure("Failed to set launcher icon: ${type.key}")
        }
    }

    override suspend fun fixLauncherIconIfNeeded(): Result<Boolean> = withContext(mainDispatcher) {
        val success = localDataSource.fixLauncherIconIfNeeded()
        if (success) {
            Result.Success(true)
        } else {
            Result.failure("Failed to fix launcher icon")
        }
    }
}
