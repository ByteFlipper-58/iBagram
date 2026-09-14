package org.telegram.messenger.feature.media.autodeletemedia.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.autodeletemedia.data.datasource.AutoDeleteMediaLocalDataSource
import org.telegram.messenger.feature.media.autodeletemedia.data.datasource.AutoDeleteMediaRemoteDataSource
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteRunResult
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteTaskState
import org.telegram.messenger.feature.media.autodeletemedia.domain.repository.AutoDeleteMediaRepository

/**
 * Clean repository coordinating auto-delete media cache retention,
 * locked file exclusions, and remote sync.
 */
class AutoDeleteMediaRepositoryImpl(
    private val localDataSource: AutoDeleteMediaLocalDataSource,
    private val remoteDataSource: AutoDeleteMediaRemoteDataSource
) : AutoDeleteMediaRepository {

    override fun observeState(): Flow<AutoDeleteTaskState> {
        return localDataSource.observeState()
    }

    override fun getState(): AutoDeleteTaskState {
        return localDataSource.getState()
    }

    override fun isFileLocked(path: String): Boolean {
        return localDataSource.isFileLocked(path)
    }

    override fun lockFile(path: String) {
        localDataSource.lockFile(path)
    }

    override fun unlockFile(path: String) {
        localDataSource.unlockFile(path)
    }

    override fun clearLockedFiles() {
        localDataSource.clearLockedFiles()
    }

    override suspend fun runCleanup(force: Boolean): Result<AutoDeleteRunResult> {
        val result = localDataSource.runCleanup(force)
        result.getOrNull()?.let {
            remoteDataSource.reportAutoDeleteAnalytics(it)
        }
        return result
    }
}
