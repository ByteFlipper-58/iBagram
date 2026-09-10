package org.telegram.messenger.feature.media.autodeletemedia.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteRunResult
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.AutoDeleteTaskState
import org.telegram.messenger.feature.media.autodeletemedia.domain.model.MediaScanFileModel
import org.telegram.messenger.feature.media.autodeletemedia.domain.repository.AutoDeleteMediaRepository

class CheckShouldRunCleanupUseCase {
    operator fun invoke(currentTimeSec: Long, lastCheckTimeSec: Long, intervalSec: Long = 86_400L): Boolean {
        if (lastCheckTimeSec <= 0) return true
        return kotlin.math.abs(currentTimeSec - lastCheckTimeSec) >= intervalSec
    }
}

class CalculateEvictionCandidatesUseCase {
    data class EvictionPlan(
        val filesToEvict: List<MediaScanFileModel>,
        val bytesFreed: Long,
        val remainingTotalBytes: Long,
        val skippedFilesCount: Int
    )

    operator fun invoke(
        allFiles: List<MediaScanFileModel>,
        maxCacheSizeBytes: Long
    ): EvictionPlan {
        var currentTotalSize = allFiles.sumOf { it.sizeBytes }
        if (currentTotalSize <= maxCacheSizeBytes) {
            return EvictionPlan(
                filesToEvict = emptyList(),
                bytesFreed = 0L,
                remainingTotalBytes = currentTotalSize,
                skippedFilesCount = 0
            )
        }

        // Sort by lastUsageTime ascending (LRU: oldest usage date first)
        val sortedCandidates = allFiles.sortedBy { it.lastUsageTimeSec }
        val toEvict = mutableListOf<MediaScanFileModel>()
        var freedBytes = 0L
        var skippedCount = 0

        for (file in sortedCandidates) {
            if (file.isLocked || file.isKeepForever) {
                // Locked or KEEP_MEDIA_FOREVER
                skippedCount++
                continue
            }
            if (file.lastUsageTimeSec <= 0) {
                skippedCount++
                continue
            }

            toEvict.add(file)
            freedBytes += file.sizeBytes
            currentTotalSize -= file.sizeBytes

            if (currentTotalSize <= maxCacheSizeBytes) {
                break
            }
        }

        return EvictionPlan(
            filesToEvict = toEvict,
            bytesFreed = freedBytes,
            remainingTotalBytes = currentTotalSize,
            skippedFilesCount = skippedCount
        )
    }
}

class LockFileUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    operator fun invoke(path: String) = repository.lockFile(path)
}

class UnlockFileUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    operator fun invoke(path: String) = repository.unlockFile(path)
}

class IsFileLockedUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    operator fun invoke(path: String): Boolean = repository.isFileLocked(path)
}

class RunAutoDeleteCleanupUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    suspend operator fun invoke(force: Boolean = false): Result<AutoDeleteRunResult> =
        repository.runCleanup(force)
}

class ObserveAutoDeleteStateUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    operator fun invoke(): Flow<AutoDeleteTaskState> = repository.observeState()
}

class GetAutoDeleteStateUseCase(
    private val repository: AutoDeleteMediaRepository
) {
    operator fun invoke(): AutoDeleteTaskState = repository.getState()
}
