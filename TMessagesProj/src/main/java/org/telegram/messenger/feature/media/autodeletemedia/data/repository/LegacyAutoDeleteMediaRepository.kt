package org.telegram.messenger.feature.media.autodeletemedia.data.repository

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
import org.telegram.messenger.feature.media.autodeletemedia.domain.repository.AutoDeleteMediaRepository
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class LegacyAutoDeleteMediaRepository(
    private val currentAccount: Int = 0,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AutoDeleteMediaRepository {

    private val localLockedPaths: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val _state = MutableStateFlow(
        AutoDeleteTaskState(
            lastCheckTimeSec = runCatching { SharedConfig.lastKeepMediaCheckTime.toLong() }.getOrDefault(0L),
            isRunning = false,
            lockedFilesCount = getLockedCount(),
            lastResult = null
        )
    )

    private fun getLockedCount(): Int {
        val all = mutableSetOf<String>()
        all.addAll(localLockedPaths)
        runCatching { all.addAll(AutoDeleteMediaTask.usingFilePaths) }
        return all.size
    }

    override fun observeState(): Flow<AutoDeleteTaskState> = _state.asStateFlow()

    override fun getState(): AutoDeleteTaskState = _state.value

    override fun isFileLocked(path: String): Boolean {
        if (localLockedPaths.contains(path)) return true
        return runCatching { AutoDeleteMediaTask.usingFilePaths.contains(path) }.getOrDefault(false)
    }

    override fun lockFile(path: String) {
        localLockedPaths.add(path)
        runCatching { AutoDeleteMediaTask.lockFile(path) }
        _state.update { it.copy(lockedFilesCount = getLockedCount()) }
    }

    override fun unlockFile(path: String) {
        localLockedPaths.remove(path)
        runCatching { AutoDeleteMediaTask.unlockFile(path) }
        _state.update { it.copy(lockedFilesCount = getLockedCount()) }
    }

    override fun clearLockedFiles() {
        localLockedPaths.clear()
        runCatching { AutoDeleteMediaTask.usingFilePaths.clear() }
        _state.update { it.copy(lockedFilesCount = 0) }
    }

    override suspend fun runCleanup(force: Boolean): Result<AutoDeleteRunResult> = withContext(ioDispatcher) {
        runCatching {
            _state.update { it.copy(isRunning = true) }
            val startTime = System.currentTimeMillis()

            if (force) {
                runCatching { SharedConfig.lastKeepMediaCheckTime = 0 }
            }

            runCatching {
                AutoDeleteMediaTask.run()
            }

            val elapsed = System.currentTimeMillis() - startTime
            val nowSec = System.currentTimeMillis() / 1000

            val result = AutoDeleteRunResult(
                filesScanned = 0,
                autoDeletedFiles = 0,
                autoDeletedBytes = 0L,
                deletedBySizeLimitFiles = 0,
                deletedBySizeLimitBytes = 0L,
                skippedFiles = 0,
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
