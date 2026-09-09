package org.telegram.messenger.feature.fileref.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.fileref.domain.model.FileRefRequestItem
import org.telegram.messenger.feature.fileref.domain.model.FileRefStatsModel

/**
 * Repository interface for managing MTProto file reference renewals,
 * parent object queries deduplication, and response caching.
 */
interface FileRefRepository {
    fun observeStats(): Flow<FileRefStatsModel>
    fun getStats(): FileRefStatsModel

    fun requestReferenceRenewal(item: FileRefRequestItem): Boolean
    fun notifyReferenceRenewed(locationKey: String, parentKey: String, refLength: Int = 0)
    fun cancelPendingRequest(locationKey: String)
    fun clearCache()
}
