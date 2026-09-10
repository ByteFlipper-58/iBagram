package org.telegram.messenger.feature.autodeletemedia.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.autodeletemedia.domain.model.AutoDeleteRunResult
import org.telegram.messenger.feature.autodeletemedia.domain.model.AutoDeleteTaskState

interface AutoDeleteMediaRepository {
    fun observeState(): Flow<AutoDeleteTaskState>
    fun getState(): AutoDeleteTaskState
    fun isFileLocked(path: String): Boolean
    fun lockFile(path: String)
    fun unlockFile(path: String)
    fun clearLockedFiles()
    suspend fun runCleanup(force: Boolean = false): Result<AutoDeleteRunResult>
}
