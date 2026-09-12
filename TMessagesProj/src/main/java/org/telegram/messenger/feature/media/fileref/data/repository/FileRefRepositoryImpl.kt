package org.telegram.messenger.feature.media.fileref.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefLocalDataSource
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefRemoteDataSource
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefCacheEntry
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefRequestItem
import org.telegram.messenger.feature.media.fileref.domain.model.FileRefStatsModel
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository

/**
 * Modern repository implementation for managing MTProto file reference renewals,
 * coordinating [FileRefLocalDataSource] and [FileRefRemoteDataSource].
 */
class FileRefRepositoryImpl(
    private val account: Int,
    private val localDataSource: FileRefLocalDataSource,
    private val remoteDataSource: FileRefRemoteDataSource
) : FileRefRepository {

    private val lock = Any()
    private val pendingRequests = LinkedHashMap<String, FileRefRequestItem>()
    private val cacheEntries = LinkedHashMap<String, FileRefCacheEntry>()

    private var totalRenewedCount: Long = 0L
    private var lastRenewalTime: Long = 0L

    private val _statsFlow = MutableStateFlow(FileRefStatsModel())
    override fun observeStats(): Flow<FileRefStatsModel> = _statsFlow.asStateFlow()
    override fun getStats(): FileRefStatsModel = _statsFlow.value

    override fun requestReferenceRenewal(item: FileRefRequestItem): Boolean {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val cached = cacheEntries[item.parentKey]
            if (cached != null && !cached.isExpired(now)) {
                totalRenewedCount++
                lastRenewalTime = now
                recomputeStatsLocked()
                return true
            }

            pendingRequests[item.locationKey] = item
            recomputeStatsLocked()
            return false
        }
    }

    override fun notifyReferenceRenewed(locationKey: String, parentKey: String, refLength: Int) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            pendingRequests.remove(locationKey)
            cacheEntries[parentKey] = FileRefCacheEntry(
                parentKey = parentKey,
                timestamp = now,
                fileReferenceBytesCount = refLength
            )
            totalRenewedCount++
            lastRenewalTime = now
            recomputeStatsLocked()
        }
    }

    override fun cancelPendingRequest(locationKey: String) {
        synchronized(lock) {
            if (pendingRequests.remove(locationKey) != null) {
                recomputeStatsLocked()
            }
        }
    }

    override fun clearCache() {
        synchronized(lock) {
            cacheEntries.clear()
            localDataSource.clearMemoryCache()
            recomputeStatsLocked()
        }
    }

    private fun recomputeStatsLocked() {
        val now = System.currentTimeMillis()
        val validCacheCount = cacheEntries.values.count { !it.isExpired(now) }
        val activeCount = pendingRequests.size

        _statsFlow.value = FileRefStatsModel(
            activeRequestsCount = activeCount,
            cachedResponsesCount = validCacheCount,
            pendingLocationsCount = activeCount,
            totalRenewedCount = totalRenewedCount,
            lastRenewalTime = lastRenewalTime
        )
    }
}
