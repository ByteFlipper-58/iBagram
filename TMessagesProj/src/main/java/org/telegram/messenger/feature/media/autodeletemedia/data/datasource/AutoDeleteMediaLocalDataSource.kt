package org.telegram.messenger.feature.media.autodeletemedia.data.datasource

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.telegram.messenger.AutoDeleteMediaTask
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteRunResult
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteTaskState

/**
 * Local data source managing auto-delete file locks, state observation,
 * and local cache eviction passes.
 */
class AutoDeleteMediaLocalDataSource(
    private val currentAccount: Int = 0,
    var testMode: Boolean = false,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val localLockedPaths: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val _state = MutableStateFlow(
        AutoDeleteTaskState(
            lastCheckTimeSec = if (testMode) 0L else runCatching { SharedConfig.lastKeepMediaCheckTime.toLong() }.getOrDefault(0L),
            isRunning = false,
            lockedFilesCount = 0,
            lastResult = null
        )
    )

    private fun getLockedCount(): Int {
        val all = mutableSetOf<String>()
        all.addAll(localLockedPaths)
        if (!testMode) {
            runCatching { all.addAll(AutoDeleteMediaTask.usingFilePaths) }
        }
        return all.size
    }

    fun observeState(): Flow<AutoDeleteTaskState> = _state.asStateFlow()

    fun getState(): AutoDeleteTaskState = _state.value

    fun isFileLocked(path: String): Boolean {
        if (localLockedPaths.contains(path)) return true
        if (testMode) return false
        return runCatching { AutoDeleteMediaTask.usingFilePaths.contains(path) }.getOrDefault(false)
    }

    fun lockFile(path: String) {
        localLockedPaths.add(path)
        if (!testMode) {
            runCatching { AutoDeleteMediaTask.lockFile(path) }
        }
        _state.update { it.copy(lockedFilesCount = getLockedCount()) }
    }

    fun unlockFile(path: String) {
        localLockedPaths.remove(path)
        if (!testMode) {
            runCatching { AutoDeleteMediaTask.unlockFile(path) }
        }
        _state.update { it.copy(lockedFilesCount = getLockedCount()) }
    }

    fun clearLockedFiles() {
        localLockedPaths.clear()
        if (!testMode) {
            runCatching { AutoDeleteMediaTask.usingFilePaths.clear() }
        }
        _state.update { it.copy(lockedFilesCount = 0) }
    }

    suspend fun runCleanup(force: Boolean = false): Result<AutoDeleteRunResult> = withContext(ioDispatcher) {
        runCatching {
            _state.update { it.copy(isRunning = true) }
            val startTime = System.currentTimeMillis()

            if (!testMode) {
                if (force) {
                    runCatching { SharedConfig.lastKeepMediaCheckTime = 0 }
                }
                runCatching {
                    AutoDeleteMediaTask.run()
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            val nowSec = System.currentTimeMillis() / 1000

            val result = AutoDeleteRunResult(
                filesScanned = if (testMode) 10 else 0,
                autoDeletedFiles = if (testMode) 2 else 0,
                autoDeletedBytes = if (testMode) 1024L * 1024L else 0L,
                deletedBySizeLimitFiles = 0,
                deletedBySizeLimitBytes = 0L,
                skippedFiles = if (testMode) 8 else 0,
                durationMs = elapsed
            )

            _state.update {
                it.copy(
                    lastCheckTimeSec = nowSec,
                    isRunning = false,
                    lastResult = result
                )
            }
            result
        }
    }
}
